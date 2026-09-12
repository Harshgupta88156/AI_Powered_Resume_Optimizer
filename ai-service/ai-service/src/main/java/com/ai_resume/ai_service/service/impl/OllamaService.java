package com.ai_resume.ai_service.service.impl;

import com.ai_resume.ai_service.client.GeminiClient;
import com.ai_resume.ai_service.dto.request.AiAnalysisRequest;
import com.ai_resume.ai_service.dto.request.MarkdownGenerationRequest;
import com.ai_resume.ai_service.dto.response.AiAnalysisResponse;
import com.ai_resume.ai_service.dto.response.MarkdownGenerationResponse;
import com.ai_resume.ai_service.service.AiService;
import com.ai_resume.ai_service.util.PromptLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OllamaService implements AiService {

    private static final String ANALYSIS_PROMPT_FILE = "ai-analysis.txt";
    private static final String ANALYSIS_PROMPT_VERSION = "prompt-v3";

    private static final String MARKDOWN_PROMPT_FILE =
            "markdown-generation.txt";
    private static final String MARKDOWN_PROMPT_VERSION = "prompt-v1";

    private final PromptLoader promptLoader;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model}")
    private String model;

    public OllamaService(
            PromptLoader promptLoader,
            GeminiClient geminiClient,
            ObjectMapper objectMapper) {
        this.promptLoader = promptLoader;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAnalysisResponse analyzeResume(AiAnalysisRequest request) {
        try {
            String template = promptLoader.loadPrompt(
                    ANALYSIS_PROMPT_FILE
            );

            String prompt = template
                    .replace(
                            "{{analysisId}}",
                            String.valueOf(request.analysisId())
                    )
                    .replace(
                            "{{resumeText}}",
                            request.resumeText()
                    )
                    .replace(
                            "{{jobDescriptionText}}",
                            request.jobDescriptionText()
                    )
                    .replace(
                            "{{company}}",
                            safeValue(request.company())
                    )
                    .replace(
                            "{{jobTitle}}",
                            safeValue(request.jobTitle())
                    );

            String generatedText = geminiClient.generateJson(prompt);
            String aiJson = cleanJson(generatedText);

            AiAnalysisResponse parsed = objectMapper.readValue(
                    aiJson,
                    AiAnalysisResponse.class
            );

            return new AiAnalysisResponse(
                    parsed.atsScore(),
                    parsed.matchScore(),
                    parsed.summary(),
                    parsed.matchingSkills(),
                    parsed.missingSkills(),
                    parsed.strengths(),
                    parsed.weaknesses(),
                    parsed.suggestedSkills(),
                    parsed.suggestions(),
                    model + "/" + ANALYSIS_PROMPT_VERSION
            );

        } catch (Exception ex) {
            String reason = ex.getMessage();
            log.error("AI resume analysis failed: {}", reason, ex);
            throw new RuntimeException(
                    "AI Resume Analysis Failed: " + reason,
                    ex
            );
        }
    }

    @Override
    public MarkdownGenerationResponse generateMarkdownResume(
            MarkdownGenerationRequest request) {
        try {
            String template = promptLoader.loadPrompt(
                    MARKDOWN_PROMPT_FILE
            );

            String prompt = template
                    .replace(
                            "{{analysisId}}",
                            String.valueOf(request.analysisId())
                    )
                    .replace(
                            "{{resumeText}}",
                            request.resumeText()
                    )
                    .replace(
                            "{{jobDescriptionText}}",
                            request.jobDescriptionText()
                    )
                    .replace(
                            "{{company}}",
                            safeValue(request.company())
                    )
                    .replace(
                            "{{jobTitle}}",
                            safeValue(request.jobTitle())
                    )
                    .replace(
                            "{{missingSkills}}",
                            joinOrNone(request.missingSkills())
                    )
                    .replace(
                            "{{suggestedSkills}}",
                            joinOrNone(request.suggestedSkills())
                    );

            log.info(
                    "Generating Markdown resume for analysis {} using Gemini {}",
                    request.analysisId(),
                    model
            );

            String generatedText = geminiClient.generate(prompt);
            String markdown = cleanMarkdown(generatedText);

            log.info(
                    "Generated Markdown resume for analysis {} with {} characters",
                    request.analysisId(),
                    markdown.length()
            );

            return new MarkdownGenerationResponse(
                    request.analysisId(),
                    markdown,
                    model + "/" + MARKDOWN_PROMPT_VERSION
            );

        } catch (Exception ex) {
            log.error(
                    "AI Markdown resume generation failed for analysis {}",
                    request.analysisId(),
                    ex
            );
            throw new RuntimeException(
                    "AI Markdown Resume Generation Failed",
                    ex
            );
        }
    }

    private String joinOrNone(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "None";
        }

        return String.join(", ", values);
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String cleanJson(String response) {
        if (response == null) {
            return "";
        }

        String cleaned = response
                .trim()
                .replaceFirst("(?is)^```json\\s*", "")
                .replaceFirst("(?is)^```\\s*", "")
                .replaceFirst("(?is)\\s*```$", "")
                .trim();

        int objectStart = cleaned.indexOf('{');
        int objectEnd = cleaned.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return cleaned.substring(objectStart, objectEnd + 1).trim();
        }
        return cleaned;
    }

    private String cleanMarkdown(String response) {
        if (response == null) {
            return "";
        }

        response = response.trim();

        response = response
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"");

        response = response.replaceAll(
                "(?i)^```markdown\\s*",
                ""
        );

        response = response.replaceAll(
                "(?i)^```md\\s*",
                ""
        );

        if (response.startsWith("```")) {
            int firstNewline = response.indexOf('\n');

            if (firstNewline != -1) {
                response = response.substring(firstNewline + 1);
            }
        }

        if (response.endsWith("```")) {
            response = response.substring(
                    0,
                    response.length() - 3
            );
        }

        return response
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}