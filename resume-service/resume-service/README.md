# Resume Service

Spring Boot microservice for resume uploads, job description uploads, and AI-based analysis.

## Features

- Upload resume files (`PDF`, `DOC`, `DOCX`) to Cloudinary
- Upload job descriptions as file (`PDF`, `DOC`, `DOCX`) or plain text
- Extract text from uploaded documents
- Trigger AI analysis using resume text + job description text
- Store and retrieve previous analyses
- Update resume metadata and delete resumes

## Configuration

Set these environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `AI_SERVICE_BASE_URL` (optional, defaults to `http://localhost:8084`)

## APIs

- `POST /api/resumes` (multipart: `file`, optional `displayName`)
- `POST /api/resumes/job-descriptions` (multipart: optional `file`, optional `text`)
- `POST /api/resumes/analyses` (JSON body with `resumeId`, `jobDescriptionId`)
- `GET /api/resumes`
- `GET /api/resumes/{id}`
- `GET /api/resumes/job-descriptions`
- `GET /api/resumes/analyses?resumeId=`
- `PUT /api/resumes/{id}`
- `DELETE /api/resumes/{id}`

## Run

```powershell
cd C:\Users\tdhakate\Downloads\Resume_optimizer\resume-service\resume-service
.\gradlew.bat bootRun
```

## Test

```powershell
cd C:\Users\tdhakate\Downloads\Resume_optimizer\resume-service\resume-service
.\gradlew.bat test
```

