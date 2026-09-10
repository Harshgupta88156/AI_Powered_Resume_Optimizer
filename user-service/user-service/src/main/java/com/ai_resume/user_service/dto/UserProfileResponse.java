package com.ai_resume.user_service.dto;

import com.ai_resume.user_service.entity.AuthProvider;
import com.ai_resume.user_service.entity.Role;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The full profile screen in one response: account fields from `users` plus
 * everything in `user_profiles`. One call instead of two keeps the page from
 * rendering half-populated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    // ── Account (from users) ──────────────────────────────────────────────
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private AuthProvider provider;
    private boolean active;
    private LocalDateTime createdAt;

    // ── Contact & location ────────────────────────────────────────────────
    private String phone;
    private String location;
    private String city;
    private String country;
    private Boolean openToRelocation;

    // ── Headline ──────────────────────────────────────────────────────────
    private String headline;
    private String summary;
    private String currentTitle;
    private String currentCompany;
    private Double totalExperienceYears;

    // ── Links ─────────────────────────────────────────────────────────────
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String leetcodeUrl;
    private String twitterUrl;

    // ── Collections ───────────────────────────────────────────────────────
    @Builder.Default
    private List<ExperienceDto> experiences = List.of();

    @Builder.Default
    private List<EducationDto> educations = List.of();

    @Builder.Default
    private List<CertificationDto> certifications = List.of();

    @Builder.Default
    private List<SkillDto> skills = List.of();

    private LocalDateTime profileUpdatedAt;

    /** Drives the "profile x% complete" nudge on the frontend. */
    private int completionPercentage;
}
