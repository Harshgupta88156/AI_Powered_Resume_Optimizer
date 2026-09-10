package com.ai_resume.trends_service.repository;

import java.time.LocalDate;

/** Raw query row for upload trend aggregation. */
public record UploadTrendRow(LocalDate bucketDate, long count) {
}

