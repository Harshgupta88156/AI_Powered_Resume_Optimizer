package com.ai_resume.resume_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A curated learning resource for one skill.
 *
 * Previously this catalog lived in a TypeScript file in the frontend bundle,
 * which meant every correction to a dead link, every new skill and every
 * reordering required a frontend rebuild and redeploy. Holding it in the
 * database lets the catalog be corrected and extended without shipping code.
 *
 * Seeded by {@link com.ai_resume.resume_service.config.LearningResourceSeeder}
 * on first boot.
 */
@Entity
@Table(
    name = "learning_resources",
    indexes = {
        @Index(name = "idx_learning_resource_skill", columnList = "skill_key"),
        @Index(name = "idx_learning_resource_category", columnList = "category")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_resource_id")
    private Long learningResourceId;

    /**
     * Normalised lookup key: lowercase, trimmed, punctuation-free
     * ("spring boot", "ci/cd" -> "ci cd"). Matching happens on this, never on
     * the display title.
     */
    @Column(name = "skill_key", nullable = false, length = 80)
    private String skillKey;

    /** Human-readable skill name shown in the UI ("Spring Boot"). */
    @Column(name = "skill_label", nullable = false, length = 80)
    private String skillLabel;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** One line on what this resource actually gives the learner. */
    @Column(name = "description", nullable = false, length = 400)
    private String description;

    @Column(name = "url", nullable = false, length = 400)
    private String url;

    /** "Official Docs", "freeCodeCamp", "Coursera", "MDN"... */
    @Column(name = "provider", nullable = false, length = 80)
    private String provider;

    /** DOCS, COURSE, VIDEO, PRACTICE, BOOK, ROADMAP. */
    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    /** BEGINNER, INTERMEDIATE, ADVANCED. */
    @Column(name = "level", nullable = false, length = 20)
    private String level;

    /** Broad grouping used to badge the card: Backend, Cloud, Frontend... */
    @Column(name = "category", nullable = false, length = 40)
    private String category;

    /** Rough time investment, for the "~6 hours" caption. Null if unknown. */
    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Builder.Default
    @Column(name = "is_free", nullable = false)
    private boolean free = true;

    /**
     * Lower sorts first within a skill. Lets the seed express "start here"
     * without depending on insertion order.
     */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
