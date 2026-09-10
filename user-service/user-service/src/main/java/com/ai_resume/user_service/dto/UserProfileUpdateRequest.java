package com.ai_resume.user_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full replacement of the profile. Every field is optional so the frontend can
 * PUT the whole form back without having to diff it.
 *
 * `role`, `email` and `active` are deliberately absent — those are not
 * self-service fields and accepting them here would be a privilege-escalation
 * hole.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    /** Display name lives on `users`, but the profile form owns it in the UI. */
    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    @Size(max = 32)
    @Pattern(
            regexp = "^$|^[+()\\-\\s0-9]{6,32}$",
            message = "Phone number may only contain digits, spaces and + - ( )")
    private String phone;

    @Size(max = 160) private String location;
    @Size(max = 100) private String city;
    @Size(max = 100) private String country;
    private Boolean openToRelocation;

    @Size(max = 160, message = "Headline must be at most 160 characters")
    private String headline;

    @Size(max = 4000, message = "Summary must be at most 4000 characters")
    private String summary;

    @Size(max = 160) private String currentTitle;
    @Size(max = 160) private String currentCompany;

    @PositiveOrZero(message = "Years of experience cannot be negative")
    private Double totalExperienceYears;

    // A blank string is allowed so a user can clear a link they previously set.
    @Pattern(regexp = URL_OR_BLANK, message = "LinkedIn link must be a valid URL")
    private String linkedinUrl;

    @Pattern(regexp = URL_OR_BLANK, message = "GitHub link must be a valid URL")
    private String githubUrl;

    @Pattern(regexp = URL_OR_BLANK, message = "Portfolio link must be a valid URL")
    private String portfolioUrl;

    @Pattern(regexp = URL_OR_BLANK, message = "LeetCode link must be a valid URL")
    private String leetcodeUrl;

    @Pattern(regexp = URL_OR_BLANK, message = "Twitter link must be a valid URL")
    private String twitterUrl;

    @Valid
    @Size(max = 50, message = "At most 50 experiences")
    private List<ExperienceDto> experiences;

    @Valid
    @Size(max = 30, message = "At most 30 education entries")
    private List<EducationDto> educations;

    @Valid
    @Size(max = 50, message = "At most 50 certificates")
    private List<CertificationDto> certifications;

    @Valid
    @Size(max = 100, message = "At most 100 skills")
    private List<SkillDto> skills;

    /**
     * Accepts a full URL or a bare host ("github.com/me"). The service
     * normalises the bare form by prefixing https://, so rejecting it here
     * would make that normalisation unreachable.
     */
    public static final String URL_OR_BLANK =
            "^$|^(https?://)?[\\w.-]+\\.[A-Za-z]{2,}(/.*)?$";
}
