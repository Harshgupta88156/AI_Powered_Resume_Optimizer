package com.ai_resume.trends_service.service;

import com.ai_resume.trends_service.dto.LabelCount;
import com.ai_resume.trends_service.dto.TimeSeriesPoint;
import com.ai_resume.trends_service.dto.TrendGranularity;
import com.ai_resume.trends_service.repository.TrendsRepository;
import com.ai_resume.trends_service.repository.UploadTrendRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrendsService {

    private final TrendsRepository trendsRepository;

    @Value("${trends.query.default-limit:10}")
    private int defaultLimit;

    @Value("${trends.query.max-limit:100}")
    private int maxLimit;

    @Value("${trends.query.max-range-days:1825}")
    private int maxRangeDays;

    public List<LabelCount> getTopMissingSkills(LocalDate from, LocalDate to, Integer limit) {
        DateRange range = resolveRange(from, to);
        return trendsRepository.topMissingSkills(range.fromTs(), range.toExclusiveTs(), resolveLimit(limit));
    }

    public List<LabelCount> getTopSuggestedSkills(LocalDate from, LocalDate to, Integer limit) {
        DateRange range = resolveRange(from, to);
        return trendsRepository.topSuggestedSkills(range.fromTs(), range.toExclusiveTs(), resolveLimit(limit));
    }

    public List<LabelCount> getTopTechnologies(LocalDate from, LocalDate to, Integer limit) {
        DateRange range = resolveRange(from, to);
        return trendsRepository.topTechnologies(range.fromTs(), range.toExclusiveTs(), resolveLimit(limit));
    }

    public List<LabelCount> getTopCompanies(LocalDate from, LocalDate to, Integer limit) {
        DateRange range = resolveRange(from, to);
        return trendsRepository.topCompanies(range.fromTs(), range.toExclusiveTs(), resolveLimit(limit));
    }

    public List<LabelCount> getTopJobTitles(LocalDate from, LocalDate to, Integer limit) {
        DateRange range = resolveRange(from, to);
        return trendsRepository.topJobTitles(range.fromTs(), range.toExclusiveTs(), resolveLimit(limit));
    }

    public List<TimeSeriesPoint> getUploadActivity(
            LocalDate from, LocalDate to, TrendGranularity granularity) {

        DateRange range = resolveRange(from, to);
        TrendGranularity resolvedGranularity = granularity == null ? TrendGranularity.WEEK : granularity;

        return trendsRepository.uploadActivity(range.fromTs(), range.toExclusiveTs(), resolvedGranularity)
                .stream()
                .map(row -> toTimeSeriesPoint(row, resolvedGranularity))
                .toList();
    }

    private TimeSeriesPoint toTimeSeriesPoint(UploadTrendRow row, TrendGranularity granularity) {
        return TimeSeriesPoint.builder()
                .date(row.bucketDate())
                .label(formatBucketLabel(row.bucketDate(), granularity))
                .count(row.count())
                .build();
    }

    private String formatBucketLabel(LocalDate date, TrendGranularity granularity) {
        return switch (granularity) {
            case DAY -> date.toString();
            case WEEK -> {
                int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = date.get(WeekFields.ISO.weekBasedYear());
                yield "%d-W%02d".formatted(year, week);
            }
            case MONTH -> "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
        };
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("'limit' must be greater than 0");
        }
        if (limit > maxLimit) {
            throw new IllegalArgumentException("'limit' must be <= " + maxLimit);
        }
        return limit;
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new DateRange(null, null);
        }

        LocalDate resolvedFrom = from == null ? to.minusDays(maxRangeDays) : from;
        LocalDate resolvedTo = to == null ? from.plusDays(maxRangeDays) : to;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }

        long spanDays = java.time.temporal.ChronoUnit.DAYS.between(resolvedFrom, resolvedTo);
        if (spanDays > maxRangeDays) {
            throw new IllegalArgumentException(
                    "Date range must not exceed " + maxRangeDays + " days");
        }

        return new DateRange(resolvedFrom.atStartOfDay(), resolvedTo.plusDays(1).atStartOfDay());
    }

    private record DateRange(LocalDateTime fromTs, LocalDateTime toExclusiveTs) {
    }
}


