package com.ai_resume.ai_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient restClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.generate-endpoint}")
    private String generateEndpoint;

    public GeminiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String generate(String prompt) {
        return generate(prompt, false);
    }

    public String generateJson(String prompt) {
        return generate(prompt, true);
    }

    private String generate(String prompt, boolean jsonResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured"
            );
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", jsonResponse
                        ? Map.of("responseMimeType", "application/json")
                        : Map.of()
        );

        JsonNode response;
        try {
            response = restClient
                    .post()
                    .uri(generateEndpoint, model)
                    .header("X-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            String providerMessage = ex.getResponseBodyAsString();
            if (providerMessage.length() > 600) {
                providerMessage = providerMessage.substring(0, 600);
            }
            throw new IllegalStateException(
                    "Gemini request failed with HTTP "
                            + ex.getStatusCode().value() + ": " + providerMessage,
                    ex
            );
        }

        if (response == null) {
            throw new IllegalStateException(
                    "Gemini returned an empty response"
            );
        }

        JsonNode generatedText = response
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (generatedText.isMissingNode() || generatedText.asText().isBlank()) {
            throw new IllegalStateException(
                    "Gemini response did not contain generated text: " + response
            );
        }

        return generatedText.asText();
    }
}