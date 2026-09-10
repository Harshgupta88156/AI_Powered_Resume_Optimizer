package com.ai_resume.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One job / internship / freelance engagement. */
@Entity
@Table(name = "user_experiences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "experience_id")
    private Long experienceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(name = "company", nullable = false, length = 160)
    private String company;

    @Column(name = "location", length = 160)
    private String location;

    /** Full-time, Internship, Contract... free text to stay flexible. */
    @Column(name = "employment_type", length = 60)
    private String employmentType;

    @Column(name = "start_date")
    private LocalDate startDate;

    /** Null while this is the current role. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Column is `is_current_role`, not `current_role`: CURRENT_ROLE is a
     * reserved keyword in PostgreSQL, so the generated DDL would fail on the
     * unquoted identifier.
     */
    @Builder.Default
    @Column(name = "is_current_role", nullable = false)
    private boolean currentRole = false;

    /** Bullet points, newline separated. TEXT because real ones are long. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
