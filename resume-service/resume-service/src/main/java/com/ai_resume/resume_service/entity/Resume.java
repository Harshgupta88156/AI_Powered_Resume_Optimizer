package com.ai_resume.resume_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A logical resume owned by a user. The actual file + extracted text live on
 * {@link ResumeVersion}, so a user can re-upload an improved CV under the same
 * resume and keep the full analysis history attached to the version it was run
 * against.
 */
@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resumes_user", columnList = "user_id")
})
@Getter
@Setter
@ToString(exclude = "versions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id")
    private Long resumeId;

    /**
     * Owner. Populated from the X-User-Id header injected by the API gateway.
     * Every read and write in this service is scoped by this column.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("versionNumber ASC")
    private List<ResumeVersion> versions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addVersion(ResumeVersion version) {
        version.setResume(this);
        this.versions.add(version);
    }

    /** Highest-numbered version, i.e. the CV the user most recently uploaded. */
    public ResumeVersion latestVersion() {
        return versions.isEmpty() ? null : versions.get(versions.size() - 1);
    }

    public int nextVersionNumber() {
        return versions.stream()
                .mapToInt(ResumeVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;
    }
}
