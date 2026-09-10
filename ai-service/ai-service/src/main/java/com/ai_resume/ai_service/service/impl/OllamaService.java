package com.ai_resume.ai_service.service.impl;

import com.ai_resume.ai_service.client.OllamaClient;
import com.ai_resume.ai_service.dto.ai.Message;
import com.ai_resume.ai_service.dto.ai.OllamaRequest;
import com.ai_resume.ai_service.dto.ai.OllamaResponse;
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

    private static final String PROMPT_FILE = "ai-analysis.txt";
    private static final String PROMPT_VERSION = "prompt-v3";
    private static final String MARKDOWN_PROMPT_FILE = "markdown-generation.txt";
    private static final String MARKDOWN_PROMPT_VERSION = "prompt-v1";

    private final PromptLoader promptLoader;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.model}")
    private String model;

    public OllamaService(
            PromptLoader promptLoader,
            OllamaClient ollamaClient,
            ObjectMapper objectMapper) {

        this.promptLoader = promptLoader;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAnalysisResponse analyzeResume(AiAnalysisRequest request) {
        try {
            String template = promptLoader.loadPrompt(PROMPT_FILE);
            String prompt = template
                    .replace("{{analysisId}}", String.valueOf(request.analysisId()))
                    .replace("{{resumeText}}", request.resumeText())
                    .replace("{{jobDescriptionText}}", request.jobDescriptionText())
                    .replace("{{company}}", safeValue(request.company()))
                    .replace("{{jobTitle}}", safeValue(request.jobTitle()));

            OllamaRequest ollamaRequest = new OllamaRequest(
                    model,
                    List.of(new Message("user", prompt)),
                    false);

            OllamaResponse response = ollamaClient.chat(ollamaRequest);
            String aiJson = cleanJson(response.message().content());
            AiAnalysisResponse parsed = objectMapper.readValue(aiJson, AiAnalysisResponse.class);

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
                    model + "/" + PROMPT_VERSION);

        } catch (Exception ex) {
            throw new RuntimeException("AI Resume Analysis Failed", ex);
        }
    }

    @Override
    public MarkdownGenerationResponse generateMarkdownResume(MarkdownGenerationRequest request) {
        try {
            String template = promptLoader.loadPrompt(MARKDOWN_PROMPT_FILE);
            String prompt = template
                    .replace("{{analysisId}}", String.valueOf(request.analysisId()))
                    .replace("{{resumeText}}", request.resumeText())
                    .replace("{{jobDescriptionText}}", request.jobDescriptionText())
                    .replace("{{company}}", safeValue(request.company()))
                    .replace("{{jobTitle}}", safeValue(request.jobTitle()))
                    .replace("{{missingSkills}}", joinOrNone(request.missingSkills()))
                    .replace("{{suggestedSkills}}", joinOrNone(request.suggestedSkills()));

            log.info("Generating Markdown resume for analysis {} using model {}", request.analysisId(), model);

            OllamaRequest ollamaRequest = new OllamaRequest(
                    model,
                    List.of(new Message("user", prompt)),
                    false);

            OllamaResponse response = ollamaClient.chat(ollamaRequest);
            String markdown = cleanMarkdown(response.message().content());

            log.info("Generated Markdown resume for analysis {} with {} characters",
                    request.analysisId(), markdown.length());

            return new MarkdownGenerationResponse(
                    request.analysisId(),
                    markdown,
                    model + "/" + MARKDOWN_PROMPT_VERSION);

        } catch (Exception ex) {
            log.error("AI Markdown Resume Generation Failed for analysis {}: {}",
                    request.analysisId(), ex.getMessage(), ex);
            throw new RuntimeException("AI Markdown Resume Generation Failed", ex);
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
        response = response.trim();
        response = response.replace("```json", "");
        response = response.replace("```", "");
        return response.trim();
    }

    /** Strips markdown code fences some models wrap their output in and normalizes escaped markdown. */
    private String cleanMarkdown(String response) {
        if (response == null) {
            return "";
        }

        response = response.trim();

        // Some models return markdown as a JSON-style escaped string.
        // Convert literal escape sequences into real formatting characters.
        response = response
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"");

        response = response.replaceAll("(?i)^```markdown\\s*", "");
        response = response.replaceAll("(?i)^```md\\s*", "");

        // Only strip a leading/trailing fence line, not every ``` that may
        // legitimately appear inside a code sample within the resume content.
        if (response.startsWith("```")) {
            int firstNewline = response.indexOf('\n');
            if (firstNewline != -1) {
                response = response.substring(firstNewline + 1);
            }
        }

        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        response = response
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return response;
    }
}