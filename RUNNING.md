# Running the app locally

Ten Spring Boot services plus an Angular frontend. You do **not** need all ten to
try the core flow — see [Minimum viable set](#minimum-viable-set).

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 | `java -version` must report 21 |
| Node.js | **≥ 22.22.3** | Angular CLI refuses to start below this |
| PostgreSQL | 14+ | running on `localhost:5432` |
| Ollama | latest | only for `ai-service` |
| Cloudinary account | free tier | **required** — `resume-service` will not start without it |

Gradle itself is not needed; every service ships a wrapper (`./gradlew`).

---

## Step 1 — Databases

Three databases. Tables are created automatically on first boot
(`ddl-auto=update`), so create the empty databases only:

```bash
psql -U postgres -c "CREATE DATABASE users_db;"
psql -U postgres -c "CREATE DATABASE auth_db;"
psql -U postgres -c "CREATE DATABASE resume_optimizer;"
```

> **Note:** `history-service` and `trends-service` read the tables that
> `resume-service` owns, so all three point at `resume_optimizer`. Their defaults
> previously pointed at a non-existent `resume_db`, which is what produced the
> `CannotGetJdbcConnectionException` in `trends-error.txt`. Fixed, but if you
> override `DB_URL` make sure those three still agree.

Default credentials are `postgres`/`postgres`, except **auth-service**, whose
committed default password is `Mahabharat@88156`. Override per service with
`DB_USERNAME`/`DB_PASSWORD` (auth-service uses `AUTH_DB_USERNAME`/`AUTH_DB_PASSWORD`).

---

## Step 2 — Ollama (the AI engine)

```bash
ollama serve          # leave running, listens on :11434
ollama pull qwen2.5   # ~4.7GB
```

Verify:

```bash
curl http://localhost:11434/api/tags
```

Without this, analysis and resume generation return `FAILED` with an error
message — the app stays up, the feature doesn't work.

---

## Step 3 — Environment variables

`resume-service` **fails to start** without Cloudinary credentials (they have no
defaults). Get them from your Cloudinary dashboard:

```bash
export CLOUDINARY_CLOUD_NAME=your_cloud_name
export CLOUDINARY_API_KEY=your_api_key
export CLOUDINARY_API_SECRET=your_api_secret
```

For GitHub login, register an OAuth App at
<https://github.com/settings/developers>:

- **Homepage URL:** `http://localhost:4200`
- **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`

> The callback goes through the **gateway on :8080**, not auth-service on :8082.
> The OAuth session cookie is scoped to whichever origin starts the handshake; a
> mismatch here fails with `authorization_request_not_found`.

```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
```

Email is optional and off by default (SMTP lines are commented out in
`notification-service`). Registration works without it.

---

## Step 4 — Start the services, in order

**Order matters for the first two.** Eureka must be up before anything else
registers, and the gateway resolves everything through Eureka.

Each in its own terminal, from the repo root:

```bash
# 1. Service registry — wait for it, then check http://localhost:8761
cd eureka-server-service/eureka-server-service && ./gradlew bootRun

# 2. API gateway
cd api-gateway-service/api-gateway-service && ./gradlew bootRun

# 3. Users
cd user-service/user-service && ./gradlew bootRun

# 4. Auth
cd auth-service/resume-optimizer && ./gradlew bootRun

# 5. AI (needs Ollama)
cd ai-service/ai-service && ./gradlew bootRun

# 6. Resumes (needs Cloudinary vars in this shell)
cd resume-service/resume-service && ./gradlew bootRun

# 7-10. Optional
cd dashboard-service/dashboard-service   && ./gradlew bootRun
cd history-service/history-service       && ./gradlew bootRun
cd trends-service/trends-service         && ./gradlew bootRun
cd notification-service/notification-service && ./gradlew bootRun
```

On Windows use `gradlew.bat`. If `./gradlew` gives "permission denied":
`chmod +x gradlew`.

| Service | Port |
|---|---|
| eureka | 8761 |
| gateway | 8080 |
| user | 8081 |
| auth | 8082 |
| resume | 8083 |
| ai | 8084 |
| dashboard | 8085 |
| history | 8086 |
| trends | 8087 |
| notification | 8088 |

Wait until <http://localhost:8761> lists every service you started before using
the app — the gateway returns 503 for services that haven't registered yet.

---

## Step 5 — Frontend

```bash
cd Frontend/ResumeOptimizerFrontend
npm install
npm start
```

Open <http://localhost:4200>. The dev server proxies nothing — the app calls the
gateway at `http://localhost:8080` directly, configured in
`src/environments/environment.ts`.

---

## Minimum viable set

To upload a resume, analyse it and download a tailored resume, you need only:

**eureka → gateway → user → auth → ai → resume** (six services), plus Postgres,
Ollama and Cloudinary.

Dashboard, History and Trends pages will error without their services; nothing
else breaks.

---

## Step 6 — Try the main flow

1. Register at <http://localhost:4200/register> (you're signed straight in).
2. **Resumes → Upload** a PDF or DOCX.
3. **Job Descriptions → Add** one — paste a real posting; the longer the better.
4. **Analysis** → pick the resume and JD → **Run analysis**. First run is slow
   (Ollama loads the model); later runs take 20-60s.
5. On the result page, **Generate tailored resume**.
6. Switch between **Preview** and **Markdown**, then **Download PDF** (choose
   "Save as PDF" in the print dialog), **Download .md**, or **.html** to edit in
   Word/Docs.

The generated resume is stored, so it's still there when you reopen the page.

---

## Verifying the build

```bash
# Frontend: type + strict template checking
cd Frontend/ResumeOptimizerFrontend
npx ngc -p tsconfig.app.json --noEmit     # exit 0 = clean

# Frontend: unit tests for the download and resume-rendering logic
npx vitest run src/app/core/utils          # 21 tests

# Frontend: component specs (needs Angular's builder)
npm test

# Backend: compile each service
cd user-service/user-service && ./gradlew compileJava
```

---

## Troubleshooting

**`resume-service` exits immediately on startup**
Cloudinary variables are missing from *that terminal's* environment. They have no
defaults. `export` them in the same shell you run `./gradlew bootRun` from.

**Gateway returns 503**
The target service hasn't registered with Eureka yet. Check <http://localhost:8761>;
registration takes up to ~30s after boot.

**GitHub login loops back to the login page, or `authorization_request_not_found`**
The callback URL in your GitHub OAuth App doesn't match
`http://localhost:8080/login/oauth2/code/github`.

**GitHub always signs in as the same account**
Should no longer happen — the app now sends `prompt=select_account`. If it
persists, you have a stale `JSESSIONID`; hard-refresh or clear cookies for
localhost.

**Analysis returns FAILED**
Ollama isn't running, or `qwen2.5` isn't pulled. Check `curl http://localhost:11434/api/tags`.

**Analysis times out**
`ollama.timeout` is 300s in `ai-service/…/application.properties`. On a slow
machine raise it, or use a smaller model (`ollama.model=qwen2.5:3b`).

**Generated resume looks thin or malformed**
That's the prompt at `ai-service/src/main/resources/prompts/markdown-generation.txt`.
The rendering path is separately tested — check the raw output in the
**Markdown** tab first to see whether the problem is generation or display.

**"Download PDF" does nothing**
It opens the browser's print dialog via a hidden iframe. If nothing appears,
check the browser blocked printing; **Download .md** always works as a fallback.

**Angular CLI refuses to start**
Node is below 22.22.3. `node -v` to confirm.

**Port already in use**
`server.port` in each service's `application.properties`, or
`SERVER_PORT=9090 ./gradlew bootRun`.

---

## A note on `DB_URL`

`user-service`, `resume-service`, `history-service` and `trends-service` all read
the same variable name `DB_URL` but need *different* values (`users_db` vs
`resume_optimizer`). That's fine when each runs in its own shell, but if you
export `DB_URL` globally you will point services at the wrong database. Prefer
setting it per-command, or leave the defaults alone.
