package com.ai_resume.user_service.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A skill the user claims. A row per skill (rather than a comma-joined column)
 * so the trends service can aggregate across users with plain SQL later.
 */
@Entity
@Table(name = "profile_skills", indexes = {
        @Index(name = "idx_profile_skills_name", columnList = "name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long skillId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** BEGINNER / INTERMEDIATE / ADVANCED / EXPERT. Optional. */
    @Column(name = "proficiency", length = 40)
    private String proficiency;
}
