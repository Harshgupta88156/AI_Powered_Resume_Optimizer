# history-service

Spring Boot service that provides timeline-friendly analysis history from the shared
`resume_optimizer` database.

## What it serves

- Full owner-scoped analysis history (`GET /api/history`)
- Focused history by resume, resume version, and job description
- Search/filter by company, title, ATS score, match score, date range, status
- Compare two analyses (`GET /api/history/compare`)

The service uses `ResumeAnalysis` as the central relationship between
`Resume`, `ResumeVersion`, and `JobDescription` and returns compact DTOs for
frontend timeline pages.

## Run

```powershell
Set-Location "C:\Users\tdhakate\Downloads\Resume_optimizer\history-service\history-service"
.\gradlew.bat bootRun
```

## Required env vars

```text
DB_URL=jdbc:postgresql://localhost:5432/resume_db
DB_USERNAME=postgres
DB_PASSWORD=Root@123
EUREKA_URL=http://localhost:8761/eureka/
```

