# trends-service

Read-only aggregation service for global trends across analyses. **Port 8087.**

## Endpoints

All endpoints are protected by the API gateway route and exposed under `/api/trends`:

- `GET /api/trends/missing-skills?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`
- `GET /api/trends/suggested-skills?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`
- `GET /api/trends/technologies?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`
- `GET /api/trends/companies?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`
- `GET /api/trends/job-titles?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`
- `GET /api/trends/upload-activity?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=WEEK`

## Design notes

- Uses plain SQL aggregations via `NamedParameterJdbcTemplate`.
- Reads from existing tables (`resume_analyses`, `resumes`, `job_descriptions`, skill child tables).
- Owns no tables and performs no writes.

## Config

```properties
trends.query.default-limit=10
trends.query.max-limit=100
trends.query.max-range-days=1825
```

