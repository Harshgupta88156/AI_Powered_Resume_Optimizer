package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.LearningResourceResponse;
import com.ai_resume.resume_service.entity.LearningResource;
import com.ai_resume.resume_service.repository.LearningResourceRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves skill names from an analysis into curated learning resources.
 *
 * Matching is deliberately forgiving: an LLM returns "Spring Boot 3",
 * "spring-boot" and "SpringBoot" for the same thing, so lookup normalises and
 * then falls back to substring matching before giving up on a skill.
 */
@Service
@RequiredArgsConstructor
public class LearningResourceService {

    private final LearningResourceRepository repository;

    /** Resources per skill, so one dominant skill cannot fill the whole list. */
    private static final int MAX_PER_SKILL = 3;

    @Transactional(readOnly = true)
    public List<LearningResourceResponse> findForSkills(Collection<String> skills, int limit) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        // Preserve caller ordering — the analysis lists the most important gap
        // first, and that should be the first recommendation shown.
        Map<String, String> keyToRequested = new LinkedHashMap<>();
        for (String skill : skills) {
            String key = normalise(skill);
            if (!key.isEmpty()) {
                keyToRequested.putIfAbsent(key, skill);
            }
        }

        if (keyToRequested.isEmpty()) {
            return List.of();
        }

        List<LearningResource> exact = repository.findForSkillKeys(keyToRequested.keySet());

        Map<String, List<LearningResource>> bySkill = new LinkedHashMap<>();
        for (LearningResource resource : exact) {
            bySkill.computeIfAbsent(resource.getSkillKey(), k -> new ArrayList<>()).add(resource);
        }

        // Anything unmatched gets a second pass against the whole catalog, so
        // "Spring Boot 3" still finds the "spring boot" entries.
        List<String> unmatched = keyToRequested.keySet().stream()
                .filter(key -> !bySkill.containsKey(key))
                .toList();

        if (!unmatched.isEmpty()) {
            List<LearningResource> all = repository.findAll();
            for (String key : unmatched) {
                for (LearningResource candidate : all) {
                    String candidateKey = candidate.getSkillKey();
                    if (key.contains(candidateKey) || candidateKey.contains(key)) {
                        bySkill.computeIfAbsent(key, k -> new ArrayList<>()).add(candidate);
                    }
                }
            }
        }

        List<LearningResourceResponse> result = new ArrayList<>();
        // De-duplicate by URL: two skills can legitimately map to one resource.
        LinkedHashSet<String> seenUrls = new LinkedHashSet<>();

        for (Map.Entry<String, String> entry : keyToRequested.entrySet()) {
            List<LearningResource> matches = bySkill.get(entry.getKey());
            if (matches == null) continue;

            int taken = 0;
            for (LearningResource resource : matches) {
                if (taken >= MAX_PER_SKILL || result.size() >= limit) break;
                if (!seenUrls.add(resource.getUrl())) continue;

                result.add(toResponse(resource, entry.getValue()));
                taken++;
            }

            if (result.size() >= limit) break;
        }

        return result;
    }

    private LearningResourceResponse toResponse(LearningResource entity, String requestedSkill) {
        return LearningResourceResponse.builder()
                // Show the label the catalog uses when we matched it, so the UI
                // reads "Spring Boot" rather than echoing "spring boot 3" back.
                .skill(entity.getSkillLabel() != null ? entity.getSkillLabel() : requestedSkill)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .url(entity.getUrl())
                .provider(entity.getProvider())
                .resourceType(entity.getResourceType())
                .level(entity.getLevel())
                .category(entity.getCategory())
                .estimatedHours(entity.getEstimatedHours())
                .free(entity.isFree())
                .build();
    }

    /** Lowercase, strip punctuation, collapse whitespace: "CI/CD" -> "ci cd". */
    private String normalise(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replaceAll("[^a-z0-9+#]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
