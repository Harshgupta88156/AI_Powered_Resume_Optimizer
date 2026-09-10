package com.ai_resume.trends_service.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One trend bucket for charting. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {

    private LocalDate date;
    private String label;
    private long count;
}

