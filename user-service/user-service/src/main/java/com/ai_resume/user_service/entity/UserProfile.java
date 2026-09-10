package com.ai_resume.user_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Extended profile, kept in its own table rather than widening `users`.
 *
 * `users` sits on the hot path of every login and every JWT validation; profile
 * data is read on exactly one screen. Splitting them keeps the auth query
 * narrow and means adding a profile field never touches the auth table.
 *
 * Every field is optional by design — a user fills this in gradually rather
 * than being blocked by a wall of required inputs.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    /**
     * Owning user. Unique, so a user can never end up with two profiles.
     * Intentionally not bidirectional: User stays free of profile concerns.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── Contact & location ────────────────────────────────────────────────

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "location", length = 160)
    private String location;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    /** Boxed so "never answered" is distinguishable from an explicit "no". */
    @Column(name = "open_to_relocation")
    private Boolean openToRelocation;

    // ── Headline / summary ────────────────────────────────────────────────

    @Column(name = "headline", length = 160)
    private String headline;

    /** TEXT, not the implicit varchar(255) — real summaries overflow that. */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "current_title", length = 160)
    private String currentTitle;

    @Column(name = "current_company", length = 160)
    private String currentCompany;

    /** Double so "2.5 years" is expressible. Null means "not stated". */
    @Column(name = "total_experience_years")
    private Double totalExperienceYears;

    // ── Links ─────────────────────────────────────────────────────────────

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    @Column(name = "leetcode_url", length = 300)
    private String leetcodeUrl;

    @Column(name = "twitter_url", length = 300)
    private String twitterUrl;

    // ── Child collections ─────────────────────────────────────────────────
    // orphanRemoval so clearing a list in the DTO actually deletes the rows
    // rather than leaving them behind with a dangling profile_id.

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Experience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Certification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProfileSkill> skills = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helpers keep both sides of each association consistent. Forgetting the
    // back-reference is the classic cause of "null profile_id" insert failures.

    public void replaceExperiences(List<Experience> replacements) {
        this.experiences.clear();
        replacements.forEach(item -> {
            item.setProfile(this);
            this.experiences.add(item);
        });
    }

    public void replaceEducations(List<Education> replacements) {
        this.educations.clear();
        replacements.forEach(item -> {
            item.setProfile(this);
            this.educations.add(item);
        });
    }

    public void replaceCertifications(List<Certification> replacements) {
        this.certifications.clear();
        replacements.forEach(item -> {
            item.setProfile(this);
            this.certifications.add(item);
        });
    }

    public void replaceSkills(List<ProfileSkill> replacements) {
        this.skills.clear();
        replacements.forEach(item -> {
            item.setProfile(this);
            this.skills.add(item);
        });
    }
}
