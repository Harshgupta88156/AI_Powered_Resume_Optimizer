package com.ai_resume.user_service.service;

import com.ai_resume.user_service.dto.CertificationDto;
import com.ai_resume.user_service.dto.EducationDto;
import com.ai_resume.user_service.dto.ExperienceDto;
import com.ai_resume.user_service.dto.SkillDto;
import com.ai_resume.user_service.dto.UserProfileResponse;
import com.ai_resume.user_service.dto.UserProfileUpdateRequest;
import com.ai_resume.user_service.entity.Certification;
import com.ai_resume.user_service.entity.Education;
import com.ai_resume.user_service.entity.Experience;
import com.ai_resume.user_service.entity.ProfileSkill;
import com.ai_resume.user_service.entity.User;
import com.ai_resume.user_service.entity.UserProfile;
import com.ai_resume.user_service.exception.ResourceNotFoundException;
import com.ai_resume.user_service.repository.UserProfileRepository;
import com.ai_resume.user_service.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Returns the profile, creating an empty one on first read so the frontend
     * never has to special-case "no profile yet".
     */
    @Transactional
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(loadOrCreate(userId));
    }

    /**
     * Full replacement. Null collections mean "leave this list alone"; an empty
     * list means "clear it". Without that distinction a form that only edits
     * the contact section would silently wipe the user's work history.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        UserProfile profile = loadOrCreate(userId);
        User user = profile.getUser();

        if (hasText(request.getName())) {
            user.setName(request.getName().trim());
            userRepository.save(user);
        }

        profile.setPhone(trimToNull(request.getPhone()));
        profile.setLocation(trimToNull(request.getLocation()));
        profile.setCity(trimToNull(request.getCity()));
        profile.setCountry(trimToNull(request.getCountry()));
        profile.setOpenToRelocation(request.getOpenToRelocation());

        profile.setHeadline(trimToNull(request.getHeadline()));
        profile.setSummary(trimToNull(request.getSummary()));
        profile.setCurrentTitle(trimToNull(request.getCurrentTitle()));
        profile.setCurrentCompany(trimToNull(request.getCurrentCompany()));
        profile.setTotalExperienceYears(request.getTotalExperienceYears());

        profile.setLinkedinUrl(normalizeUrl(request.getLinkedinUrl()));
        profile.setGithubUrl(normalizeUrl(request.getGithubUrl()));
        profile.setPortfolioUrl(normalizeUrl(request.getPortfolioUrl()));
        profile.setLeetcodeUrl(normalizeUrl(request.getLeetcodeUrl()));
        profile.setTwitterUrl(normalizeUrl(request.getTwitterUrl()));

        if (request.getExperiences() != null) {
            profile.replaceExperiences(request.getExperiences().stream()
                    .filter(dto -> hasText(dto.getJobTitle()) && hasText(dto.getCompany()))
                    .map(this::toEntity)
                    .toList());
        }

        if (request.getEducations() != null) {
            profile.replaceEducations(request.getEducations().stream()
                    .filter(dto -> hasText(dto.getInstitution()))
                    .map(this::toEntity)
                    .toList());
        }

        if (request.getCertifications() != null) {
            profile.replaceCertifications(request.getCertifications().stream()
                    .filter(dto -> hasText(dto.getName()))
                    .map(this::toEntity)
                    .toList());
        }

        if (request.getSkills() != null) {
            profile.replaceSkills(dedupeSkills(request.getSkills()));
        }

        return toResponse(userProfileRepository.save(profile));
    }

    // =====================================================================
    // Loading
    // =====================================================================

    private UserProfile loadOrCreate(Long userId) {
        Optional<UserProfile> existing = userProfileRepository.findFullByUserId(userId);
        if (existing.isPresent()) {
            UserProfile profile = existing.get();
            initialiseCollections(profile);
            return profile;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return userProfileRepository.save(UserProfile.builder().user(user).build());
    }

    /**
     * Touches each child collection so it is loaded while the transaction is
     * still open. `spring.jpa.open-in-view=false` means the session is closed by
     * the time the DTO mapper runs, so an untouched lazy list would throw
     * LazyInitializationException.
     *
     * Four separate selects, not one join - see the note on
     * {@link com.ai_resume.user_service.repository.UserProfileRepository#findFullByUserId}.
     */
    private void initialiseCollections(UserProfile profile) {
        profile.getExperiences().size();
        profile.getEducations().size();
        profile.getCertifications().size();
        profile.getSkills().size();
    }

    // =====================================================================
    // Mapping
    // =====================================================================

    private Experience toEntity(ExperienceDto dto) {
        boolean current = Boolean.TRUE.equals(dto.getCurrentRole());
        return Experience.builder()
                .jobTitle(dto.getJobTitle().trim())
                .company(dto.getCompany().trim())
                .location(trimToNull(dto.getLocation()))
                .employmentType(trimToNull(dto.getEmploymentType()))
                .startDate(dto.getStartDate())
                // A role cannot be both current and ended - the end date wins
                // only when the flag is off.
                .endDate(current ? null : dto.getEndDate())
                .currentRole(current)
                .description(trimToNull(dto.getDescription()))
                .build();
    }

    private Education toEntity(EducationDto dto) {
        return Education.builder()
                .institution(dto.getInstitution().trim())
                .degree(trimToNull(dto.getDegree()))
                .fieldOfStudy(trimToNull(dto.getFieldOfStudy()))
                .location(trimToNull(dto.getLocation()))
                .startYear(dto.getStartYear())
                .endYear(dto.getEndYear())
                .grade(trimToNull(dto.getGrade()))
                .description(trimToNull(dto.getDescription()))
                .build();
    }

    private Certification toEntity(CertificationDto dto) {
        return Certification.builder()
                .name(dto.getName().trim())
                .issuingOrganization(trimToNull(dto.getIssuingOrganization()))
                .credentialId(trimToNull(dto.getCredentialId()))
                .credentialUrl(normalizeUrl(dto.getCredentialUrl()))
                .issueDate(dto.getIssueDate())
                .expiryDate(dto.getExpiryDate())
                .build();
    }

    /** Case-insensitive dedupe, preserving the order the user typed them in. */
    private List<ProfileSkill> dedupeSkills(List<SkillDto> skills) {
        Set<String> seen = new LinkedHashSet<>();
        List<ProfileSkill> result = new ArrayList<>();

        for (SkillDto dto : skills) {
            if (!hasText(dto.getName())) {
                continue;
            }
            String name = dto.getName().trim();
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            result.add(ProfileSkill.builder()
                    .name(name)
                    .proficiency(trimToNull(dto.getProficiency()))
                    .build());
        }
        return result;
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        User user = profile.getUser();

        List<ExperienceDto> experiences = profile.getExperiences().stream()
                .map(e -> ExperienceDto.builder()
                        .experienceId(e.getExperienceId())
                        .jobTitle(e.getJobTitle())
                        .company(e.getCompany())
                        .location(e.getLocation())
                        .employmentType(e.getEmploymentType())
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .currentRole(e.isCurrentRole())
                        .description(e.getDescription())
                        .build())
                .toList();

        List<EducationDto> educations = profile.getEducations().stream()
                .map(e -> EducationDto.builder()
                        .educationId(e.getEducationId())
                        .institution(e.getInstitution())
                        .degree(e.getDegree())
                        .fieldOfStudy(e.getFieldOfStudy())
                        .location(e.getLocation())
                        .startYear(e.getStartYear())
                        .endYear(e.getEndYear())
                        .grade(e.getGrade())
                        .description(e.getDescription())
                        .build())
                .toList();

        List<CertificationDto> certifications = profile.getCertifications().stream()
                .map(c -> CertificationDto.builder()
                        .certificationId(c.getCertificationId())
                        .name(c.getName())
                        .issuingOrganization(c.getIssuingOrganization())
                        .credentialId(c.getCredentialId())
                        .credentialUrl(c.getCredentialUrl())
                        .issueDate(c.getIssueDate())
                        .expiryDate(c.getExpiryDate())
                        .build())
                .toList();

        List<SkillDto> skills = profile.getSkills().stream()
                .map(s -> SkillDto.builder()
                        .name(s.getName())
                        .proficiency(s.getProficiency())
                        .build())
                .toList();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .phone(profile.getPhone())
                .location(profile.getLocation())
                .city(profile.getCity())
                .country(profile.getCountry())
                .openToRelocation(profile.getOpenToRelocation())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .currentTitle(profile.getCurrentTitle())
                .currentCompany(profile.getCurrentCompany())
                .totalExperienceYears(profile.getTotalExperienceYears())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .leetcodeUrl(profile.getLeetcodeUrl())
                .twitterUrl(profile.getTwitterUrl())
                .experiences(experiences)
                .educations(educations)
                .certifications(certifications)
                .skills(skills)
                .profileUpdatedAt(profile.getUpdatedAt())
                .build();

        response.setCompletionPercentage(calculateCompletion(response));
        return response;
    }

    /**
     * Twelve equally-weighted signals. Rough on purpose: it exists to nudge the
     * user toward a fuller profile, not to score them precisely.
     */
    private int calculateCompletion(UserProfileResponse p) {
        boolean[] signals = {
                hasText(p.getName()),
                hasText(p.getPhone()),
                hasText(p.getLocation()) || hasText(p.getCity()),
                hasText(p.getHeadline()),
                hasText(p.getSummary()),
                hasText(p.getCurrentTitle()),
                p.getTotalExperienceYears() != null,
                hasText(p.getLinkedinUrl()),
                hasText(p.getGithubUrl()),
                !p.getExperiences().isEmpty(),
                !p.getEducations().isEmpty(),
                !p.getSkills().isEmpty()
        };

        int filled = 0;
        for (boolean signal : signals) {
            if (signal) filled++;
        }
        return Math.round((filled * 100f) / signals.length);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Accepts "github.com/me" as well as a full URL - users paste both, and a
     * href without a scheme resolves relative to the app and 404s.
     */
    private String normalizeUrl(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }
}
