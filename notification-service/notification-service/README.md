# notification-service

EMAIL-ONLY notification microservice for Resume Optimizer.

## Responsibilities

- Sends transactional/informational emails via SMTP (Spring Boot Mail + Thymeleaf HTML templates).
- Does **not** own a database, does not store notification history/preferences, and does not
  compute any business data itself (e.g. it never calculates weekly trends - it only renders
  and sends what `trends-service` already computed).

## Endpoints

All endpoints return `202 ACCEPTED` immediately; the actual SMTP send happens asynchronously
on a dedicated thread pool (`emailTaskExecutor`) so a slow/misconfigured mail server can never
block or fail the caller's business operation (registration, analysis, weekly trends job).

| Method | Path                              | Called by         | Trigger                                   |
|--------|-----------------------------------|--------------------|--------------------------------------------|
| POST   | `/api/notifications/welcome`            | auth-service      | Successful user registration               |
| POST   | `/api/notifications/analysis-completed` | resume-service    | ai-service analysis completes successfully |
| POST   | `/api/notifications/weekly-trends`      | trends-service     | Weekly `@Scheduled` job                    |

## Configuration

All SMTP settings are externalized via environment variables (see `application.properties`):
`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NAME`,
`FRONTEND_BASE_URL`.

## Switching email providers

`EmailService` is the only abstraction the rest of the service depends on. `SmtpEmailService`
implements it with `JavaMailSender`. To move to Amazon SES/SendGrid/Brevo/Mailgun, add a new
`EmailService` implementation and mark it `@Primary` (or remove the SMTP one) - no other class
needs to change.

