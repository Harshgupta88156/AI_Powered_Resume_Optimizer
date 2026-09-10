package com.ai_resume.trends_service.repository;

import com.ai_resume.trends_service.dto.LabelCount;
import com.ai_resume.trends_service.dto.TrendGranularity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrendsRepository implements TrendsRepository {

    private static final RowMapper<LabelCount> LABEL_COUNT_MAPPER =
            (rs, rowNum) -> LabelCount.builder()
                    .label(rs.getString("label"))
                    .count(rs.getLong("count"))
                    .build();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTrendsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<LabelCount> topMissingSkills(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit) {
        String sql = """
                SELECT MIN(BTRIM(ms.skill)) AS label, COUNT(*) AS count
                FROM analysis_missing_skills ms
                JOIN resume_analyses ra ON ra.analysis_id = ms.analysis_id
                WHERE BTRIM(ms.skill) <> ''
                  AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                  AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                GROUP BY LOWER(BTRIM(ms.skill))
                ORDER BY count DESC, label ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, limit), LABEL_COUNT_MAPPER);
    }

    @Override
    public List<LabelCount> topSuggestedSkills(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit) {
        String sql = """
                SELECT MIN(BTRIM(ss.skill)) AS label, COUNT(*) AS count
                FROM analysis_suggested_skills ss
                JOIN resume_analyses ra ON ra.analysis_id = ss.analysis_id
                WHERE BTRIM(ss.skill) <> ''
                  AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                  AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                GROUP BY LOWER(BTRIM(ss.skill))
                ORDER BY count DESC, label ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, limit), LABEL_COUNT_MAPPER);
    }

    @Override
    public List<LabelCount> topTechnologies(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit) {
        String sql = """
                SELECT MIN(skill_label) AS label, COUNT(*) AS count
                FROM (
                    SELECT LOWER(BTRIM(ms.skill)) AS skill_key, BTRIM(ms.skill) AS skill_label
                    FROM analysis_missing_skills ms
                    JOIN resume_analyses ra ON ra.analysis_id = ms.analysis_id
                    WHERE BTRIM(ms.skill) <> ''
                      AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                      AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                    UNION ALL
                    SELECT LOWER(BTRIM(ss.skill)) AS skill_key, BTRIM(ss.skill) AS skill_label
                    FROM analysis_suggested_skills ss
                    JOIN resume_analyses ra ON ra.analysis_id = ss.analysis_id
                    WHERE BTRIM(ss.skill) <> ''
                      AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                      AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                ) combined
                GROUP BY skill_key
                ORDER BY count DESC, label ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, limit), LABEL_COUNT_MAPPER);
    }

    @Override
    public List<LabelCount> topCompanies(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit) {
        String sql = """
                SELECT MIN(BTRIM(jd.company)) AS label, COUNT(*) AS count
                FROM resume_analyses ra
                JOIN job_descriptions jd ON jd.job_description_id = ra.job_description_id
                WHERE jd.company IS NOT NULL
                  AND BTRIM(jd.company) <> ''
                  AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                  AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                GROUP BY LOWER(BTRIM(jd.company))
                ORDER BY count DESC, label ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, limit), LABEL_COUNT_MAPPER);
    }

    @Override
    public List<LabelCount> topJobTitles(LocalDateTime fromTs, LocalDateTime toExclusiveTs, int limit) {
        String sql = """
                SELECT MIN(BTRIM(jd.job_title)) AS label, COUNT(*) AS count
                FROM resume_analyses ra
                JOIN job_descriptions jd ON jd.job_description_id = ra.job_description_id
                WHERE jd.job_title IS NOT NULL
                  AND BTRIM(jd.job_title) <> ''
                  AND (CAST(:fromTs AS timestamp) IS NULL OR ra.created_at >= CAST(:fromTs AS timestamp))
                  AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR ra.created_at < CAST(:toExclusiveTs AS timestamp))
                GROUP BY LOWER(BTRIM(jd.job_title))
                ORDER BY count DESC, label ASC
                LIMIT :limit
                """;
        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, limit), LABEL_COUNT_MAPPER);
    }

    @Override
    public List<UploadTrendRow> uploadActivity(
            LocalDateTime fromTs, LocalDateTime toExclusiveTs, TrendGranularity granularity) {

        String truncUnit = switch (granularity) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
        };

        String sql = """
                SELECT DATE_TRUNC('%s', r.created_at)::date AS bucket_date,
                       COUNT(*) AS count
                FROM resumes r
                WHERE (CAST(:fromTs AS timestamp) IS NULL OR r.created_at >= CAST(:fromTs AS timestamp))
                  AND (CAST(:toExclusiveTs AS timestamp) IS NULL OR r.created_at < CAST(:toExclusiveTs AS timestamp))
                GROUP BY bucket_date
                ORDER BY bucket_date ASC
                """.formatted(truncUnit);

        return jdbcTemplate.query(sql, rangeParams(fromTs, toExclusiveTs, null),
                (rs, rowNum) -> new UploadTrendRow(
                        rs.getObject("bucket_date", LocalDate.class),
                        rs.getLong("count")));
    }

    private MapSqlParameterSource rangeParams(LocalDateTime fromTs, LocalDateTime toExclusiveTs, Integer limit) {
        // PostgreSQL's JDBC driver cannot infer the parameter type when a null value is bound
        // without an explicit SQL type, and throws "could not determine data type of parameter".
        // Since from/to are almost always null (no date filter applied), every query without a
        // date range used to fail. Passing java.sql.Types explicitly avoids the type inference.
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTs", fromTs, java.sql.Types.TIMESTAMP)
                .addValue("toExclusiveTs", toExclusiveTs, java.sql.Types.TIMESTAMP);

        if (limit != null) {
            params.addValue("limit", limit, java.sql.Types.INTEGER);
        }
        return params;
    }
}

