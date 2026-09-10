package com.ai_resume.resume_service.dto;

import com.ai_resume.resume_service.entity.JobDescriptionSource;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescriptionResponse {

    private Long jobDescriptionId;
    private String company;
    private String jobTitle;
    private JobDescriptionSource source;
    private String fileName;
    private String contentType;
    private String cloudinaryUrl;
    private LocalDateTime createdAt;

    /**
     * The parsed job description text.
     *
     * Populated only by the single-item endpoint. The list endpoint leaves it
     * null and sends {@link #textPreview} instead - twenty full job
     * descriptions is a large payload to ship for a page that only renders a
     * couple of lines of each.
     */
    private String extractedText;

    /** Short excerpt, always populated, so the list can show what a JD contains. */
    private String textPreview;

    /** Character count of the full text, for the "1,240 characters" caption. */
    private Integer textLength;
}
