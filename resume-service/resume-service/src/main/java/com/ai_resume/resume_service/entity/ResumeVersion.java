package com.ai_resume.resume_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One concrete uploaded file belonging to a {@link Resume}.
 *
 * <p>Analyses point at a version rather than at the resume itself, which is what
 * makes "ATS improvement over time" and version-to-version comparison possible
 * without ever mutating historical records.
 */
@Entity
@Table(name = "resume_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resume_version_number",
                columnNames = {"resume_id", "version_number"}),
        indexes = {
                @Index(name = "idx_resume_versions_resume", columnList = "resume_id")
        })
@Getter
@Setter
@ToString(exclude = {"resume", "extractedText"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_version_id")
    private Long resumeVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    /** 1-based, monotonically increasing within a resume. */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "cloudinary_url", nullable = false, length = 1024)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", nullable = false, length = 512)
    private String cloudinaryPublicId;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "TEXT")
    private String extractedText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

