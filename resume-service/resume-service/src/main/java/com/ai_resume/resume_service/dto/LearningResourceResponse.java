package com.ai_resume.resume_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningResourceResponse {

    /** The skill this was recommended for, as the UI should label it. */
    private String skill;

    private String title;
    private String description;
    private String url;
    private String provider;

    /** DOCS, COURSE, VIDEO, PRACTICE, BOOK, ROADMAP. */
    private String resourceType;

    /** BEGINNER, INTERMEDIATE, ADVANCED. */
    private String level;

    private String category;
    private Integer estimatedHours;
    private boolean free;
}
