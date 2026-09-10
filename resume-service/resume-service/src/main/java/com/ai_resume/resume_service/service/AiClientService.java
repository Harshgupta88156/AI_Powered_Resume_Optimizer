package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.AiAnalysisRequest;
import com.ai_resume.resume_service.dto.AiAnalysisResponse;
import com.ai_resume.resume_service.dto.MarkdownGenerationRequest;
import com.ai_resume.resume_service.dto.MarkdownResumeResponse;
import com.ai_resume.resume_service.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for ai-service.
 *
 * <p>ai-service is currently a skeleton with no controller, so every call here
 * fails. That is expected and handled: {@code ResumeService} records the analysis
 * with status FAILED instead of blowing up the request. When ai-service ships
 * with {@code POST /api/ai/analyze} honouring {@link AiAnalysisResponse}, nothing
 * in this class needs to change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiClientService {

    /** Service id registered in Eureka — resolved by the load-balanced RestTemplate. */
    private final RestTemplate loadBalancedRestTemplate;

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai.service.analyze-endpoint}")
    private String analyzeEndpoint;

    @Value("${ai.service.generate-markdown-endpoint}")
    private String generateMarkdownEndpoint;

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        String url = aiServiceBaseUrl + analyzeEndpoint;
        try {
            ResponseEntity<AiAnalysisResponse> response =
                    loadBalancedRestTemplate.postForEntity(url, request, AiAnalysisResponse.class);

            AiAnalysisResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new ExternalServiceException(
                        "AI service returned " + response.getStatusCode() + " with no usable body", null);
            }
            return body;
        } catch (RestClientException ex) {
            log.error("Failed to call AI service at {}: {}", url, ex.getMessage());
            throw new ExternalServiceException("AI service is unavailable: " + ex.getMessage(), ex);
        }
    }

    public MarkdownResumeResponse generateMarkdown(MarkdownGenerationRequest request) {
        String url = aiServiceBaseUrl + generateMarkdownEndpoint;
        try {
            ResponseEntity<MarkdownResumeResponse> response =
                    loadBalancedRestTemplate.postForEntity(url, request, MarkdownResumeResponse.class);

            MarkdownResumeResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new ExternalServiceException(
                        "AI service returned " + response.getStatusCode() + " with no usable body", null);
            }
            return body;
        } catch (RestClientException ex) {
            log.error("Failed to call AI service (generate-markdown) at {}: {}", url, ex.getMessage());
            throw new ExternalServiceException("AI service is unavailable: " + ex.getMessage(), ex);
        }
    }
}

