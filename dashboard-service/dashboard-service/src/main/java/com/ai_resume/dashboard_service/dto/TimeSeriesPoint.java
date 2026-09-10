package com.ai_resume.dashboard_service.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One point on a chart.
 *
 * <p>{@code value} is a {@code Double} so counts and averages share one shape and
 * the frontend can render every series with a single component.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {

    /** Start of the bucket: the day, the Monday of the ISO week, or the 1st of the month. */
    private LocalDate date;

    /** Human-readable bucket label, e.g. "2026-W31" or "2026-07". */
    private String label;

    private Double value;
}

