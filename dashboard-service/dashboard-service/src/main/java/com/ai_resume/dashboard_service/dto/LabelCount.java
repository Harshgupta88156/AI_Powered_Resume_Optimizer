package com.ai_resume.dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A "top N" row: a label and how many times it occurred. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCount {

    private String label;
    private long count;
}

