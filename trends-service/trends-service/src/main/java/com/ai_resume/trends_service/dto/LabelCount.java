package com.ai_resume.trends_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A top-N aggregation row. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCount {

    private String label;
    private long count;
}

