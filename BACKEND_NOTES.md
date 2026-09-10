# Backend notes — state after the stabilisation pass

## What was fixed

### Data-loss / guaranteed-failure bugs
| Fix | Why it mattered |
|---|---|
| `job_descriptions.extracted_text` → `TEXT` | Was an implicit `varchar(255)`. Any real JD threw `DataIntegrityViolationException` on insert. |
| `resume_analyses.analysis_result` → structured columns + `TEXT` | Same overflow, guaranteed on every real AI result. |
| Cloudinary `public_id` now has a random suffix | Two users uploading `resume.pdf` produced the same id; the second upload **overwrote the first user's file**. |
| File bytes read once and passed to Cloudinary | `CloudinaryService` re-read `file.getBytes()` after the extractor had consumed the part. |
| `JobDescription.source` enum replaces `textInput` | The old boolean was `nullable = false` but never assigned — permanently `false`. |
| JD Cloudinary assets deleted on delete | JD files were **never** removed; storage grew forever. |
| Refresh-token purge job | Every login + every refresh rotation inserted a row that was never deleted. |

### Security
| Fix | Why it mattered |
|---|---|
| `user_id` on `resumes` / `job_descriptions` / `resume_analyses`, every query scoped by it | Any authenticated user could `GET /api/resumes` and read, or `DELETE`, **everyone's** resumes. |
| `X-User-Id` required on every resume-service endpoint | resume-service had no Spring Security at all and was wide open on :8083. |
| `StripUserHeadersGlobalFilter` at the gateway | Without it a client could send `X-User-Id: 1` and impersonate anyone. This is what makes the header trustworthy. |
| `role` removed from `RegisterRequest` / `UserUpdateRequest`; `Role` enum; ADMIN-only `PATCH /users/{id}/role` | `{"role":"ADMIN"}` at registration was accepted → instant privilege escalation. |
| `@EnableMethodSecurity` + `@PreAuthorize` on user endpoints | `GET /api/users` dumped every user's email to any logged-in user. |
| `InternalApiKeyFilter` on the 3 internal user endpoints | They were `permitAll`; anyone reaching :8081 could create accounts and brute-force passwords. |
| JWT `issuer` now verified in all three validators | |
| CORS at the gateway | No browser client could call the API at all. |

### Robustness
- Timeouts on **all** outbound calls (Feign → user-service, RestTemplate → ai-service). Previously "wait forever".
- `AiClientService` uses the **Eureka service id** (`http://ai-service`) via `@LoadBalanced`, not a hardcoded `localhost:8084`.
- Multipart size limits + gateway codec buffer.
- Logged catch-all exception handlers in all three services (no more leaked stack traces).
- `MissingRequestHeaderException` → **401**, unreadable document → **422**, delete-with-history → **409**.
- `DocumentTextExtractor` no longer re-wraps its own `InvalidFileTypeException` into a 500.
- `spring.jpa.open-in-view=false` everywhere; page sizes capped at 100.
- Removed all `System.out.println`, commented-out duplicate methods, and the redundant post-save `findById` re-read.
- Entities use `@Getter/@Setter` instead of `@Data` (Lombok `equals`/`hashCode` over lazy associations is an entity-identity bug).

---

## Forward-compatible design for the planned services

These are **not implemented**. The schema and APIs were shaped so they can be
built without another migration.

### `ResumeAnalysis` is the fact table
Everything the Dashboard, History and Trends features need is already recorded:

```
resume_analyses
  analysis_id, user_id, resume_id, resume_version_id, job_description_id,
  status, ats_score, match_score, overall_summary,
  raw_result, error_message, engine_version, created_at, completed_at

analysis_missing_skills   (analysis_id, skill)    -- indexed
analysis_suggested_skills (analysis_id, skill)    -- indexed
analysis_suggestions      (analysis_id, suggestion)
```

Skills are **child tables, not a JSON blob**, specifically so Trends can do
`SELECT skill, COUNT(*) FROM analysis_missing_skills GROUP BY skill ORDER BY 2 DESC`
with no parsing and no separately-maintained trend table — exactly as you
specified.

### 1. Dashboard service
Every counter you listed already has a repository method or a trivial query:

| Metric | Source |
|---|---|
| Total resumes | `ResumeRepository.countByUserId` |
| Total resume versions | `ResumeVersionRepository.countByResume_UserId` |
| Total analyses | `ResumeAnalysisRepository.countByUserId` |
| Total job descriptions | `JobDescriptionRepository.countByUserId` |
| Average ATS / match score | `AVG(ats_score)`, `AVG(match_score)` where `status='COMPLETED'` |
| Weekly / monthly counts | `GROUP BY date_trunc(...) ON created_at` (indexed) |
| ATS improvement over time | `ats_score` ordered by `created_at`, joined to `resume_version_number` |
| Recent uploads / analyses | existing paginated, `createdAt DESC`-sorted endpoints |
| Unread notifications | notification-service |

### 2. History service
The model you described is already in place:
`Resume 1—N ResumeVersion 1—N ResumeAnalysis N—1 JobDescription`.

`AnalysisResponse` already carries resume, version number, JD, **company**,
**job title**, date, ATS score, match score, missing skills, suggested skills,
suggestions, summary and status — i.e. the full history record.

Supported today: history for a user / resume / version / job description, with
sorting and pagination (`GET /api/resumes/analyses?resumeId=&resumeVersionId=&jobDescriptionId=&page=&size=&sort=`).

Still to build: company/title search, score and date-range filters, and
compare-two-analyses. All are query-layer work — **no schema change needed**.

Analyses are treated as **append-only**: deleting a resume or JD that has
analyses returns `409 Conflict` rather than destroying history.

### 3. Notification service — IMPLEMENTED (email-only)
Deliberately much smaller than the plan below implied. Scope, by design:
**EMAIL ONLY** - no notification DB, no history, no preferences, no in-app/push/
SMS, no message broker. Frontend toasts (upload success, login success, etc.)
stay purely client-side and never touch this service.

- `notification-service` (port 8088) owns all SMTP logic (Spring Boot Mail) and
  Thymeleaf HTML templates (`welcome`, `analysis-completed`, `weekly-trends`).
- `EmailService` is the only abstraction; swapping SMTP for SES/SendGrid/Brevo/
  Mailgun later means adding one new implementation, nothing else changes.
- Every `/api/notifications/*` endpoint returns `202 ACCEPTED` immediately;
  the actual send happens on an async executor (`EmailDispatcher` /
  `emailTaskExecutor`) so a slow/broken mail server can never block the caller.
- Callers wrap the outbound call in try/catch and only log on failure:
  - `auth-service` → `POST /api/notifications/welcome` after registration (Feign `NotificationClient`).
  - `resume-service` → `POST /api/notifications/analysis-completed` once an analysis reaches `COMPLETED` (load-balanced `RestTemplate`, same pattern as `AiClientService`).
  - `trends-service` → `POST /api/notifications/weekly-trends` from a `@Scheduled` job (`WeeklyTrendsScheduler`, default `0 0 7 * * MON`). trends-service computes the data; notification-service only renders and sends it.
- Registered in Eureka, routed at the gateway under `/api/notifications/**`
  (JWT-protected, mainly for ops/testing — day-to-day usage is service-to-service).


### 4. Trends service (global, not per-user)
Read-only aggregations over `resume_analyses` + the three skill child tables.
Because those tables are indexed on `skill` and on `created_at`, date-range
filtering and top-N queries are plain SQL. **Do not** add trend tables until a
measured performance problem appears — as you specified.

### 5. Profile service
`users` now has `provider`, `active`, `updated_at`. `GET/PUT /api/users/me`
exist. Extended profile fields belong in a new `user_profiles` table keyed by
`user_id` rather than widening `users`.

---

## ai-service (deliberately not implemented)

The contract is fixed and resume-service already speaks it. When you build it,
implement exactly:

`POST /api/ai/analyze`

Request (`AiAnalysisRequest`):
```json
{ "analysisId": 1, "resumeText": "...", "jobDescriptionText": "...",
  "company": "Acme", "jobTitle": "Backend Engineer" }
```

Response (`AiAnalysisResponse`):
```json
{ "atsScore": 78, "matchScore": 64,
  "summary": "...",
  "missingSkills": ["Kubernetes"],
  "suggestedSkills": ["Docker"],
  "suggestions": ["Quantify impact in your last role"],
  "engineVersion": "gemini-1.5-pro/prompt-v1" }
```

Until then `POST /api/resumes/analyses` returns **202 Accepted** with
`status: "FAILED"` and an `errorMessage`, and the row is still persisted — so
history stays consistent and nothing 500s.

---

## Known remaining gaps (intentionally deferred)

1. **No Flyway** — still `ddl-auto=update`. See `MIGRATION.md`; adopt Flyway before production.
2. **HS256 shared secret** — all services can mint tokens. Move to RS256 + JWKS.
3. **No Config Server** — properties are duplicated per service.
4. **No `common-lib`** — `JwtService`, DTOs and `GlobalExceptionHandler` are still duplicated across services.
5. **Analysis is synchronous** — fine now; move to Kafka + `202`/polling when ai-service is real.
6. **No circuit breaker / rate limiting / Actuator / OpenAPI.**
7. **Tests are still context-load only.**

