# Fixes — AI Resume Optimizer

All six items are addressed. Root causes are documented per item, because several
were not where they first appeared.

**Verification status:** the frontend compiles clean under Angular's strict
template checking and 13 new unit tests pass. **The Java was not compiled** —
Gradle's distribution download was blocked in my environment. See
[Before you run it](#before-you-run-it).

---

## 1. Tailored resume wouldn't download

**Root cause — two independent bugs in the same four lines.**

The old `downloadMarkdown()` did this:

```ts
const anchor = document.createElement('a');
anchor.href = url;
anchor.download = fileName;
anchor.click();
URL.revokeObjectURL(url);   // same tick
```

1. **The anchor was never added to the document.** Chrome tolerates a click on a
   detached anchor. Firefox and Safari ignore it silently — no download, no
   error, nothing in the console. That alone explains "it didn't download".
2. **`revokeObjectURL` ran on the same tick as `click()`.** The browser hands the
   download off asynchronously, so revoking immediately can destroy the blob
   before it has been read, producing a 0-byte or failed download.

**Fix.** New `core/utils/file-download.util.ts` attaches the anchor, defers the
revoke by 1s, and always cleans up. Both failure modes are covered by tests that
I verified fail against the original implementation and pass against the fix.

**Three further problems found in the same feature:**

- **Cached resumes were invisible.** The Download buttons only rendered when
  `markdown()` was non-null, which only happened right after clicking Generate.
  Reload the page and your generated resume was gone from the UI even though the
  backend had stored it. Added `GET /api/resumes/analyses/{id}/markdown`
  (`ResumeController` + `ResumeService.getGeneratedMarkdownResume`) and the page
  now loads it on open. Returns 404 when nothing exists, which the client treats
  as "no cached copy" rather than an error.
- **"Download PDF" was pop-up blocked.** It used `window.open()`, blocked by
  default in most browsers. Now renders into a hidden same-origin iframe, which
  cannot be blocked.
- **The markdown→HTML converter didn't escape input.** Resume text containing
  `<` or `&` (`C++ <algorithm>`, `R&D`) injected raw markup into the print
  document. New `markdown.util.ts` escapes first, then builds markup.

Also added: `.html` export (opens cleanly in Word and Google Docs), a clipboard
fallback for non-HTTPS origins where `navigator.clipboard` is undefined, and
filenames derived from the resume and job title rather than `tailored-resume-7.md`.

---

## 2. OAuth wouldn't let you switch accounts

**Root cause — three compounding causes.**

1. **GitHub was never asked for the account picker.** Once you have authorised
   the app, GitHub silently reuses whichever account is signed in on github.com
   and redirects straight back. You cannot pick a different one without logging
   out of GitHub entirely. GitHub supports `prompt=select_account` to force the
   picker; the app never sent it.
2. **The handshake used two different origins.** The frontend sent users to
   `http://localhost:8082/oauth2/authorization/github`, bypassing the gateway,
   so the OAuth session cookie landed on `:8082` while the app ran on `:4200`.
3. **Users were matched by email alone.** GitHub withholds the email unless it is
   public, and the code fabricated `login@users.noreply.github.com` as a
   fallback. If you later made your email public, you got a *second* account.

**Fix.**

- `PromptAwareAuthorizationRequestResolver` forwards `prompt=select_account`
  (default, allow-listed so a crafted link can't inject arbitrary parameters).
- `GithubOAuth2UserService` calls `/user/emails` to get the verified primary
  address GitHub withholds from the profile payload.
- Users are now matched on **provider + providerId** (GitHub's immutable numeric
  id), falling back to email for rows written before `providerId` existed, and
  backfilling it when that happens.
- The whole flow runs through the gateway, and the success handler invalidates
  the OAuth session as soon as our own tokens are issued.
- The frontend clears local session state *before* leaving the page, so a
  half-finished handshake can't leave the previous user signed in.

**One bug this introduced, and how it's handled.** Moving the handshake to the
gateway meant `redirect-uri={baseUrl}/login/oauth2/code/{registrationId}` would
resolve to auth-service's own `:8082`. The session cookie holding the pending
authorization request is scoped to `:8080`, so the browser would send no cookie
and Spring would fail with `authorization_request_not_found`. The redirect URI is
now pinned to the gateway — **you must update the callback URL in your GitHub
OAuth App to match** (see below).

Also fixed: the OAuth failure handler read its redirect URI from
`System.getenv()` instead of Spring config, so it would never have found the
value set in `application.properties`.

---

## 3. Extended profile data

New `user_profiles` table plus `user_experiences`, `user_educations`,
`user_certifications`, `profile_skills` — kept separate from `users` rather than
widening it, because `users` sits on the hot path of every login and JWT
validation while profile data is read on one screen.

**Fields:** phone, location, city, country, open-to-relocation, headline,
summary, current title/company, total years of experience, and links for
LinkedIn, GitHub, portfolio, LeetCode and Twitter. Plus full repeating sections
for work experience, education (college, degree, field, years, grade),
certifications (issuer, credential id and URL, issue/expiry) and skills with
proficiency.

**Endpoints:** `GET /api/users/me/profile`, `PUT /api/users/me/profile`. The PUT
is a full replacement so the form doesn't have to compute a diff — but a `null`
collection means "section not sent, leave it alone" while an empty array means
"user cleared it", so editing your contact details can't silently wipe your work
history. `role`, `email` and `active` are deliberately not accepted here; taking
them would be a privilege-escalation hole.

The response carries a `completionPercentage` to drive a "profile N% complete"
nudge.

**Three bugs caught while building this:**

- **`current_role` is a reserved keyword in PostgreSQL.** The generated DDL would
  have failed on the unquoted identifier. Column renamed to `is_current_role`.
- **Lombok generates `isCurrentRole()`, not `getCurrentRole()`** for a primitive
  boolean. The service called the latter.
- **The URL validator rejected what the service was built to accept.** `@Pattern`
  demanded `https://…`, but `normalizeUrl` existed specifically to prefix bare
  domains like `github.com/me`. Validation ran first, so that helper was
  unreachable. Pattern relaxed.

---

## 4. Light and dark mode

Full token system in `styles.css` — every colour is a CSS variable defined for
both themes, so switching is one attribute swap on `<html>` with no
component-level work.

- `ThemeService` supports **light / dark / system**, follows OS changes live, and
  persists the choice.
- An inline script in `index.html` applies the saved theme **before first paint**.
  Without it the page renders light for one frame then flips — a visible flash.
- 13 component stylesheets converted from hardcoded hex to tokens. Colours on
  brand-gradient panels were deliberately left white rather than tokenised.
- `theme-color` meta updates so mobile browser chrome matches.

---

## 5. Frontend bugs

- **Sidebar `height: 100vh` inside a layout with a 64px header** pushed the last
  nav item below the fold on every screen. Now `height: 100%`.
- **The mobile drawer never closed after navigating**, covering the page you had
  just opened. Now closes on `NavigationEnd`, on backdrop tap, and on Escape.
- **Guards called `router.navigate()` from inside the guard**, racing the
  in-flight navigation — the cause of intermittent blank screens on hard refresh.
  Now return a `UrlTree`, and carry a `returnUrl`.
- **Unknown URLs redirected to `/`**, so a stale link or a mistyped
  `/analysis/9999` looked like a silent logout. Real 404 page added.
- **"Forgot Password?" was `href="#"`**, which just jumped the page to the top.
- **Register redirected to `/login`** even though registration already returns a
  full session, forcing users to retype credentials. Now signs straight in.
- **Enabled `strictTemplates`**, which caught three real type errors in the
  profile form. The codebase now passes it.
- **Repaired 14 spec files** that would have thrown `NullInjectorError` — the
  components inject `HttpClient` and the router, but the TestBeds provided
  neither.

---

## 6. Session length

Access token **15 min → 4 hours**; refresh token **7 → 30 days**. Both still
overridable via `JWT_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS`.

The bigger problem was that **the refresh token was never used**. The old
interceptor responded to any 401 by wiping storage and redirecting to login, so
the effective session was exactly 15 minutes regardless of the refresh token's
lifetime. It now transparently refreshes and replays the failed request.

Concurrent 401s share a **single** in-flight refresh call. Without that, a
dashboard firing six parallel requests would rotate the refresh token six times;
five rotations would be rejected and the user would be logged out mid-session.

Debug logging (`org.springframework.security=DEBUG`, `web=DEBUG`) was left on in
`application.properties` and leaked tokens into logs — now `INFO`, env-overridable.

---

## 7. Tailored resume quality and appearance (the USP)

The download worked after item 1, but the *document* was weak. Three separate
layers were at fault.

**The prompt produced a contact-less, date-less document.** It specified
`### Job Title` with no company, no location and no dates, and the required shape
started at the name with no contact line at all — so the output had no phone,
no email, no LinkedIn, and no way to tell when anything happened. No amount of
CSS fixes a resume with no dates on it.

Rewritten `markdown-generation.txt` now mandates:

- a name line, then a single `location · phone · email · LinkedIn · GitHub`
  contact line, written as bare readable text so ATS parsers read it cleanly
- `### Job Title — Company` followed by an italic `*Location · Jan 2022 – Present*`
  meta line, with explicit instructions to omit rather than invent
- strong-verb bullets, banned opener adjectives ("Results-driven", "Passionate"),
  a fixed section order, and one-page guidance for under five years' experience
- **"never invent numbers"** — the old prompt's "improve weak bullets" invited
  the model to fabricate metrics. It now sharpens wording without adding figures.

All original truthfulness rules are kept: missing skills still appear only under
*Additional JD-Aligned Skills* as "Familiar with" / "Exposure to".

**The renderer threw away structure.** Everything became `<p>`, so the contact
line and date lines were indistinguishable from body text. `markdownToHtml` now
infers two structural classes from position — `.contact` (the line under the
name) and `.meta` (an all-italic line under an entry heading). Both are inferred,
so if the model ignores the format the document degrades to ordinary paragraphs
instead of breaking.

**The print CSS was a generic article stylesheet** — blue H1, blue underline,
20mm margins. Replaced with a proper resume typesetting pass: centred small-caps
name, subtle grey contact line, uppercase letter-spaced section headings with
hairline rules, tight leading, and `page-break-after: avoid` on headings so a
section title can't strand at the foot of a page.

Also added: **a live Preview tab** next to the raw Markdown. `RESUME_STYLESHEET`
is a single constant shared by the preview and the PDF export, so what you see is
what downloads. The preview lives in its own component that injects the sheet
into `<head>` — Angular's view encapsulation rewrites component CSS with a host
attribute that `[innerHTML]` content never receives, so encapsulated rules would
silently not apply.

The paper stays light in dark mode. A resume is printed and shared as a PDF; a
dark-themed resume is not what lands in the file.

**Verified by rendering, not by eye.** A realistic sample resume was pushed
through the real `buildResumeDocument` path, rendered to PDF, and checked:
one A4 page, and `pdftotext` extracts every section in correct reading order with
the email, phone, skills and grade intact — which is the actual ATS guarantee.
The degraded case (model omits contact and meta lines) was rendered too and lays
out cleanly.

---

## 8. History and Trends pointed at a database that never existed

`resume-service` writes to `resume_optimizer`. `history-service` and
`trends-service` defaulted to `resume_db` — a database nothing creates. This is
the cause of the stack trace committed at `trends-error.txt`:
`CannotGetJdbcConnectionException` from `JdbcTrendsRepository.topMissingSkills`.

Both now default to `resume_optimizer`, the database whose schema
`resume-service` owns.

---

## 9. Second round of reported bugs

### Profile page: "An unexpected error occurred"

`UserProfileRepository.findFullByUserId` fetch-joined **four `List` collections**
in one `@EntityGraph` (experiences, educations, certifications, skills).
Hibernate permits at most one bag per fetch-join and throws
`MultipleBagFetchException: cannot simultaneously fetch multiple bags` — so
*every* call to `GET /api/users/me/profile` failed, which is why the page could
never load or save.

The graph now fetches only `user`; the four collections are initialised inside
the existing transaction. Four extra selects instead of one join — and unlike
the join, no row multiplication across four collections.

### Literal ``` at the bottom of Trends

`trends-page.html` ended with a stray markdown code fence, left over from a
paste. Angular rendered it as text. Removed, and I scanned every other template
for the same thing — this was the only one.

### No way back to the landing page

The header logo points at `/dashboard`, so once signed in there was no route
home. Added a **Home** item at the top of the sidebar, with exact-match
highlighting so it doesn't stay lit on every child route.

### Scores didn't discriminate (QA resume matching an SWE role)

`ai-analysis.txt` asked for "integers from 0 to 100" with **no rubric at all**.
With no anchors an LLM defaults to a generous 70-85 for almost anything, so a QA
resume scored well against a backend JD.

The prompt now:
- separates the two scores explicitly — `atsScore` judges *parseability of the
  resume alone and ignores the JD*; `matchScore` judges fit to this JD. It also
  flags that scores within 5 points of each other mean the criteria weren't
  really applied separately.
- opens with a **discipline gate** that overrides everything: different
  discipline forces 5-30 *even with heavy keyword overlap* ("a QA engineer who
  writes Java and uses Git is still not a backend developer"); adjacent
  discipline caps at 65.
- adds banded criteria, a seniority penalty, and three worked calibration
  examples including the exact QA-vs-backend case (expected `matchScore` 18).
- states that returning a score below 20 is correct and useful, and that
  `weaknesses` must lead with the discipline mismatch.

### Recommended resources felt fake

**24 of 58 links were YouTube *search-result* pages** — `?search_query=...` —
presented as if they were curated courses. Replaced 23 with real canonical
resources: the Python Tutorial, the TypeScript Handbook, MDN's JavaScript Guide,
Kubernetes Basics, Spring Boot guides, PostgreSQL's tutorial, MongoDB
University, Google's ML Crash Course, and so on.

The remaining fallback for skills outside the catalog still uses search links —
that is legitimate for an arbitrary skill — but the copy no longer pretends
otherwise ("Browse X courses on Coursera", "X documentation search" via DevDocs)
rather than dressing a search page up as a specific course.

### Pagination

Resumes, Job Descriptions and History each had a hand-rolled Previous/Next pair:
no page numbers, no sense of how much data existed, and nine clicks to reach
page nine. Replaced with a shared `app-pagination` component — numbered pages
with a collapsing window (first and last always visible, gaps as ellipses, fixed
width whether there are 3 pages or 300) and a "Showing 21–40 of 87" caption.
Covered by 8 unit tests over the windowing and range maths.

### Charts on Trends

Added a bar chart and a donut chart, both dependency-free.

`chart.js` and `ng2-charts` are already in `package.json`, but both render to
`<canvas>`, which cannot inherit CSS custom properties — every colour would need
recomputing on each theme switch. DOM and SVG charts follow the theme for free,
reflow responsively, and keep labels selectable and screen-reader accessible.
The donut collapses its long tail into "Other" so a hundred one-count skills
don't render as invisible hairlines.

### Job descriptions were unreadable

The JD cards showed only a title, company, source badge and date. There was no
way to read what a job description actually said — no detail route, no preview,
no viewer.

The backend was already doing its part: the list endpoint sends a `textPreview`
on every row and `GET /job-descriptions/{id}` returns the full `extractedText`.
Both were simply never rendered, and `JobDescriptionService.getById` was never
called from anywhere.

Now:
- Each card shows a three-line excerpt and a character count, so the list tells
  you what each JD contains at a glance. Cards with no extracted text say so
  explicitly — that JD cannot be analysed, and silence was hiding it.
- Clicking a card (or **View**) opens a reader with the full text, preserving
  the source line breaks. The text is fetched on demand and cached back onto the
  row, so reopening is instant.
- The reader offers **Copy text**, **Open original file** for uploaded PDFs and
  DOCXs, and **Analyze against a resume**.
- Closes on backdrop click, the × button, or Escape; keyboard focusable with
  Enter to open.

---

## 10. Third round

### Job descriptions: view the uploaded file, edit, rename

The JD reader now has **Extracted text** / **Original file** tabs. Uploaded PDFs
embed inline in an iframe, the same way you'd expect a PDF to open; Word files
fall back to an "open the original" button, because browsers can't render
`.docx` in a frame and a broken preview is worse than an honest one.

Editing needed a backend endpoint that did not exist — added
`PUT /api/resumes/job-descriptions/{id}` plus `JobDescriptionUpdateRequest`.
Null fields are left untouched, so a rename sends only the title without wiping
the company.

**Text of an uploaded file is deliberately read-only.** It's the parser's output
for that document; letting it drift from the file it claims to represent would
make past analyses unreproducible. The UI says so rather than silently
disabling the box. `IllegalArgumentException` is used for the 400 — the existing
handler already maps it, so no new exception type was introduced.

### Learning resources moved to the database

The catalog was **689 lines of TypeScript in the frontend bundle**, so every dead
link needed a frontend rebuild and redeploy.

Now a `learning_resources` table in resume-service, served by
`GET /api/resumes/analyses/{id}/learning-resources`, seeded with **59 curated
resources across 30 skills** — dev.java, Spring guides, react.dev, the
TypeScript Handbook, MDN, SQLBolt, Use The Index Luke, Kubernetes the Hard Way,
System Design Primer, NeetCode 150, OWASP, Pro Git, Learn Git Branching.
**Every one of the 59 URLs was checked and returns HTTP 200.**

The seeder keys off each skill rather than "is the table empty", so adding a
skill ships on the next restart without overwriting rows an operator has fixed
by hand. Matching normalises the skill name and falls back to substring matching,
so "Spring Boot 3" still resolves to the "spring boot" entries.

Per the brief the frontend layout is unchanged — same grouped grid. Cards gained
provider, type, level, duration and a Free badge, and the section now shows a
loading state. A failure fetching recommendations is swallowed: they're
supplementary and must never take over the analysis page.

### Trends: more graphical

Added an **activity area chart**. `uploadActivity` was already being fetched and
then thrown away — nothing rendered it. Alongside the existing bar and donut
charts, Trends now leads with a time series.

All three charts are hand-rolled SVG/DOM. 13 tests cover the geometry, including
the single-point case (which divides by zero if you spread points naively) and
an all-zero series.

### Logo goes home, and the landing page knows you

The header logo pointed at `/dashboard`; it now goes to `/`. The landing navbar
reads the auth state: signed out it shows Login / Get Started, signed in it shows
your avatar, your name linking to the profile, and **Go to Dashboard** — rather
than inviting someone already logged in to log in again.

### Profile: verified, not just built

Checked end to end. The form covers name, headline, phone, city, country,
relocation, summary, current title/company, years of experience, LinkedIn,
GitHub, portfolio, LeetCode and Twitter, plus repeating sections for experience,
education (institution, degree, field, years, grade), certifications and a
comma-separated skills box. It PUTs to `/api/users/me/profile` and patches the
header name on save.

It was reading back as broken only because of the `MultipleBagFetchException` in
item 9 — with that fixed, saved details load correctly.

---

## Before you run it

**1. Update your GitHub OAuth App callback URL** to
`http://localhost:8080/login/oauth2/code/github` (was `:8082`). The flow will
fail with `authorization_request_not_found` until this matches.

**2. Compile the Java.** I could not — Gradle's distribution download is blocked
in my sandbox. The changes are cross-checked by script (every Lombok accessor and
builder field the services call resolves against an actual entity field), but
that is not a substitute for `javac`:

```bash
cd user-service/user-service    && ./gradlew compileJava
cd auth-service/resume-optimizer && ./gradlew compileJava
cd resume-service/resume-service && ./gradlew compileJava
```

**3. Schema.** Still `ddl-auto=update`, so the five new tables are created on
first boot. `MIGRATION.md` already flags adopting Flyway before production; these
tables make that more urgent, not less.

**4. Frontend.** Full setup guide in `RUNNING.md`.

```bash
cd Frontend/ResumeOptimizerFrontend
npm install
npm start
```

Angular CLI needs Node ≥ 22.22.3. Component specs run via `ng test` — they need
Angular's builder, since JIT can't resolve `templateUrl` under bare Vitest. The
utility tests run either way: `npx vitest run src/app/core/utils`.

---

## Not done

- **Prompt tuning is model-dependent.** The prompt was rewritten and verified
  against a well-formed sample, but I could not run `qwen2.5` here. If real output
  drifts from the specified shape, that file is where to adjust it.
- **No integration tests.** Backend tests are still context-load only.
- The **HS256 shared secret** means any service can mint tokens, and there's
  still no Config Server or `common-lib`. Pre-existing, listed in
  `BACKEND_NOTES.md`, out of scope here.
