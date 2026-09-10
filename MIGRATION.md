# Schema migration — REQUIRED before starting the fixed backend

`spring.jpa.hibernate.ddl-auto=update` **adds** columns and tables but never
changes a column's type, never drops a column, and never relaxes a `NOT NULL`
constraint. Several of the fixes in this pass do exactly those things, so
Hibernate cannot apply them for you.

Run the SQL below once, then start the services.

---

## Option A — clean reset (recommended for dev)

```sql
-- resume_optimizer database
DROP TABLE IF EXISTS analysis_missing_skills   CASCADE;
DROP TABLE IF EXISTS analysis_suggested_skills CASCADE;
DROP TABLE IF EXISTS analysis_suggestions      CASCADE;
DROP TABLE IF EXISTS resume_analyses           CASCADE;
DROP TABLE IF EXISTS resume_versions           CASCADE;
DROP TABLE IF EXISTS job_descriptions          CASCADE;
DROP TABLE IF EXISTS resumes                   CASCADE;

-- user_db database
DROP TABLE IF EXISTS users CASCADE;

-- auth_db database
DROP TABLE IF EXISTS refresh_tokens CASCADE;
```

Hibernate recreates everything correctly on the next startup.

---

## Option B — preserve existing data

### `resume_optimizer`

```sql
-- 1. Widen the columns that were silently varchar(255).
--    Any real job description or AI result overflowed these.
ALTER TABLE job_descriptions ALTER COLUMN extracted_text TYPE TEXT;

-- 2. Ownership. Backfill with a real user id before enforcing NOT NULL.
ALTER TABLE resumes           ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE job_descriptions  ADD COLUMN IF NOT EXISTS user_id BIGINT;
UPDATE resumes          SET user_id = <EXISTING_USER_ID> WHERE user_id IS NULL;
UPDATE job_descriptions SET user_id = <EXISTING_USER_ID> WHERE user_id IS NULL;
ALTER TABLE resumes          ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE job_descriptions ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_resumes_user ON resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_jd_user      ON job_descriptions(user_id);

-- 3. Job description metadata (needed by history + trends).
ALTER TABLE job_descriptions ADD COLUMN IF NOT EXISTS company   VARCHAR(255);
ALTER TABLE job_descriptions ADD COLUMN IF NOT EXISTS job_title VARCHAR(255);
ALTER TABLE job_descriptions ADD COLUMN IF NOT EXISTS source    VARCHAR(16);
UPDATE job_descriptions
   SET source = CASE WHEN is_text_input THEN 'TEXT' ELSE 'FILE' END
 WHERE source IS NULL;
ALTER TABLE job_descriptions ALTER COLUMN source SET NOT NULL;
ALTER TABLE job_descriptions DROP COLUMN IF EXISTS is_text_input;

-- 4. Resume versioning: move the file columns off `resumes` onto `resume_versions`.
CREATE TABLE IF NOT EXISTS resume_versions (
    resume_version_id    BIGSERIAL PRIMARY KEY,
    resume_id            BIGINT       NOT NULL REFERENCES resumes(resume_id),
    version_number       INTEGER      NOT NULL,
    file_name            VARCHAR(255) NOT NULL,
    content_type         VARCHAR(255) NOT NULL,
    file_size_bytes      BIGINT,
    cloudinary_url       VARCHAR(1024) NOT NULL,
    cloudinary_public_id VARCHAR(512)  NOT NULL,
    extracted_text       TEXT          NOT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uk_resume_version_number UNIQUE (resume_id, version_number)
);

INSERT INTO resume_versions (
    resume_id, version_number, file_name, content_type,
    cloudinary_url, cloudinary_public_id, extracted_text, created_at)
SELECT resume_id, 1, file_name, content_type,
       cloudinary_url, cloudinary_public_id, extracted_text, created_at
FROM resumes
WHERE NOT EXISTS (SELECT 1 FROM resume_versions v WHERE v.resume_id = resumes.resume_id);

ALTER TABLE resumes DROP COLUMN IF EXISTS file_name;
ALTER TABLE resumes DROP COLUMN IF EXISTS content_type;
ALTER TABLE resumes DROP COLUMN IF EXISTS cloudinary_url;
ALTER TABLE resumes DROP COLUMN IF EXISTS cloudinary_public_id;
ALTER TABLE resumes DROP COLUMN IF EXISTS extracted_text;
ALTER TABLE resumes ADD  COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE resumes ALTER COLUMN notes TYPE TEXT;

-- 5. Structured analysis record.
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS user_id           BIGINT;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS resume_version_id BIGINT;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS status            VARCHAR(16);
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS ats_score         INTEGER;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS match_score       INTEGER;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS overall_summary   TEXT;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS raw_result        TEXT;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS error_message     TEXT;
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS engine_version    VARCHAR(64);
ALTER TABLE resume_analyses ADD COLUMN IF NOT EXISTS completed_at      TIMESTAMP;

UPDATE resume_analyses a
   SET user_id           = r.user_id,
       resume_version_id = (SELECT MIN(v.resume_version_id)
                              FROM resume_versions v
                             WHERE v.resume_id = a.resume_id),
       raw_result        = a.analysis_result,
       status            = 'COMPLETED'
  FROM resumes r
 WHERE r.resume_id = a.resume_id
   AND a.user_id IS NULL;

ALTER TABLE resume_analyses ALTER COLUMN user_id           SET NOT NULL;
ALTER TABLE resume_analyses ALTER COLUMN resume_version_id SET NOT NULL;
ALTER TABLE resume_analyses ALTER COLUMN status            SET NOT NULL;
ALTER TABLE resume_analyses DROP COLUMN IF EXISTS analysis_result;

CREATE INDEX IF NOT EXISTS idx_analysis_user    ON resume_analyses(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_created ON resume_analyses(created_at);

-- 6. Skill child tables (Hibernate will also create these automatically).
CREATE TABLE IF NOT EXISTS analysis_missing_skills (
    analysis_id BIGINT NOT NULL REFERENCES resume_analyses(analysis_id),
    skill       VARCHAR(255) NOT NULL);
CREATE TABLE IF NOT EXISTS analysis_suggested_skills (
    analysis_id BIGINT NOT NULL REFERENCES resume_analyses(analysis_id),
    skill       VARCHAR(255) NOT NULL);
CREATE TABLE IF NOT EXISTS analysis_suggestions (
    analysis_id BIGINT NOT NULL REFERENCES resume_analyses(analysis_id),
    suggestion  TEXT   NOT NULL);
```

### `user_db`

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS active     BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE users SET active   = TRUE    WHERE active   IS NULL;
UPDATE users SET provider = 'LOCAL' WHERE provider IS NULL;
-- role/provider are now enums persisted as STRING; normalise any stray values
UPDATE users SET role = 'USER' WHERE role NOT IN ('USER', 'ADMIN');
UPDATE users SET provider = UPPER(provider);
ALTER TABLE users ALTER COLUMN active   SET NOT NULL;
ALTER TABLE users ALTER COLUMN provider SET NOT NULL;
```

### `auth_db`

```sql
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS name VARCHAR(255);
```

---

## Required environment variables

```
JWT_SECRET=<random, >= 32 bytes>
INTERNAL_API_KEY=<random>        # must be IDENTICAL in auth-service and user-service
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

`INTERNAL_API_KEY` is new. If auth-service and user-service disagree, **every
login and registration returns 401** — that is the single most likely cause of a
"nothing works" symptom after this upgrade.

