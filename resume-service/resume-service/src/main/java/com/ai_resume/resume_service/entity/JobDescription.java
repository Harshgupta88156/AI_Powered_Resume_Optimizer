package com.ai_resume.resume_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A job description a user wants their resume measured against.
 *
 * <p>{@code company} and {@code jobTitle} are first-class columns because the
 * history and trends features search and group by them.
 */
@Entity
@Table(name = "job_descriptions", indexes = {
        @Index(name = "idx_jd_user", columnList = "user_id"),
        @Index(name = "idx_jd_company", columnList = "company"),
        @Index(name = "idx_jd_job_title", columnList = "job_title")
})
@Getter
@Setter
@ToString(exclude = "extractedText")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_description_id")
    private Long jobDescriptionId;

    /** Owner of this JD record. Scopes every read/write in this service. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company")
    private String company;

    @Column(name = "job_title")
    private String jobTitle;

    /** Replaces the old boolean {@code textInput} flag, which was never assigned. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private JobDescriptionSource source;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "cloudinary_url", length = 1024)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", length = 512)
    private String cloudinaryPublicId;

    /**
     * A real job description is several thousand characters; the previous
     * implicit varchar(255) made every non-trivial upload fail on insert.
     */
    @Column(name = "extracted_text", nullable = false, columnDefinition = "TEXT")
    private String extractedText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

