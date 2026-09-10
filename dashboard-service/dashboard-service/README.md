# dashboard-service

Frontend-facing aggregator (BFF) for the Resume Optimizer dashboard. **Port 8085.**

## Design

It **owns no database and no business rules.** There is deliberately no
`spring-boot-starter-data-jpa` and no JDBC driver in `build.gradle` — if one
appears, that is a design regression.

It reads from the services that own the data, through their **existing public
APIs**. No endpoint was added to resume-service for the dashboard's benefit:

```
                  ┌──────────────────────┐
  GET /api/dashboard ─►│  dashboard-service   │
                  │  (stateless, no DB)  │
                  └───────┬──────────┬───┘
                          │          │
              existing REST APIs     │ (not built yet)
                          ▼          ▼
                  resume-service   notification-service
```

Identity is never decided here. The gateway validates the JWT and injects
`X-User-Id`; `FeignConfig` forwards it downstream, so **resume-service applies
exactly the same ownership scoping it applies to a direct call**. Strip the
header and the request fails with 401 — the correct failure mode.

### One snapshot per request

`DashboardDataLoader` produces a single immutable `DashboardData` record.
`SummaryStatistics`, `DashboardCharts` and `DashboardInsights` are **pure
functions of that snapshot**, so they physically cannot issue their own calls.
Adding a fifth panel later means reading from the snapshot, not adding a fifth
fan-out.

The snapshot is cached per user for 30s (Caffeine), keyed on `X-User-Id`.

## Endpoints

All require `X-User-Id` (injected by the gateway).

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dashboard` | Whole page in one request |
| GET | `/api/dashboard/summary` | Counter cards |
| GET | `/api/dashboard/recent-activity` | Recent resumes / analyses / notifications |
| GET | `/api/dashboard/charts` | All chart series |
| GET | `/api/dashboard/insights` | Top-N lists |

**Query params:** `from`, `to` (ISO dates), `granularity` (`DAY`\|`WEEK`\|`MONTH`,
default `WEEK`), `recentLimit` (default 5, max 25), `insightLimit` (default 10,
max 50). Default range is the last 180 days; max span is 5 years.

### Coverage of the spec

| Requested | Field |
|---|---|
| Total resumes | `summary.totalResumes` |
| Total resume versions | `summary.totalResumeVersions` |
| Total analyses | `summary.totalAnalyses` |
| Average ATS score | `summary.averageAtsScore` |
| Average match score | `summary.averageMatchScore` |
| Total job descriptions | `summary.totalJobDescriptions` |
| Total unread notifications | `summary.totalUnreadNotifications` |
| Recently uploaded resumes | `recentActivity.recentResumes` |
| Recent analyses | `recentActivity.recentAnalyses` |
| Recent notifications | `recentActivity.recentNotifications` |
| Weekly analysis count | `charts.weeklyAnalysisCount` |
| Monthly analysis count | `charts.monthlyAnalysisCount` |
| ATS improvement over time | `charts.atsImprovement` (+ `matchImprovement`) |
| Resume upload trend | `charts.resumeUploadTrend` |
| Top missing skills | `insights.topMissingSkills` |
| Top recommended skills | `insights.topRecommendedSkills` |
| Top technologies | `insights.topTechnologies` |
| Most analyzed companies | `insights.mostAnalyzedCompanies` (+ `mostAnalyzedJobTitles`) |

## Behaviour worth knowing

- **Empty buckets are emitted as zeros.** Skipping them makes a line chart lie:
  two points a month apart get drawn adjacent, implying continuous activity.
- **Averages are `null`, not `0`, when there is no data.** Showing `0` to a user
  who has never completed an analysis reads as a terrible score, not "no data".
- **Score averages count only `COMPLETED` analyses**, so `FAILED` runs (which is
  all of them until ai-service ships) never drag the numbers down.
- **Skills are counted case-insensitively**, reported with the first spelling
  seen — otherwise `React`/`react`/`REACT` occupy three slots in a top-10.
- **Ties break alphabetically**, so the order is stable between reloads.
- **`topTechnologies`** is derived from the missing + recommended skill lists
  rather than a hand-maintained technology dictionary, which would be guesswork
  that rots. When ai-service returns a dedicated technologies list, feed it in;
  the API shape does not change.

## Failure modes

| Dependency | If it's down |
|---|---|
| resume-service | **503** with `source: "resume-service"` — there is no meaningful dashboard without it, so this is not silently hidden. |
| notification-service | Dashboard renders normally; notification fields are empty and the service is listed in `degradedSources`. |

`notification-service` does not exist yet. `NotificationServiceClient` is the
agreed contract (`GET /api/notifications/unread-count`, `GET /api/notifications`);
calls are off by default. Set `dashboard.notifications.enabled=true` once it is
registered in Eureka — no code change needed.

## Configuration

```properties
dashboard.fetch.page-size=100        # must be <= resume-service max-page-size
dashboard.fetch.max-analyses=1000    # ceiling on history pulled per render
dashboard.fetch.max-resumes=500
dashboard.notifications.enabled=false
dashboard.cache.ttl-seconds=30       # 0 disables
feign.connect-timeout-ms=3000
feign.read-timeout-ms=10000
```

## Known limitation: bounded aggregation

Averages, charts and insights are computed **in this service** from analyses
pulled over HTTP, capped at `dashboard.fetch.max-analyses`. When a user exceeds
the cap, `DashboardData.truncated` is set — the figures then describe a recent
window rather than all time.

This is the honest cost of "aggregate, don't duplicate": dashboard-service owns
no tables, so it cannot run `AVG`/`GROUP BY` in SQL. `summary.totalResumes`,
`totalAnalyses` and `totalJobDescriptions` are exempt — they come from
`totalElements` on a `size=1` page and are always exact.

If this becomes a bottleneck, the fix is **not** to give this service a database.
It is to have resume-service expose pre-aggregated projections and have
`DashboardDataLoader` consume those instead. The response DTOs and the frontend
contract would not change.

