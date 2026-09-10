package com.ai_resume.ai_service.client;

import com.ai_resume.ai_service.dto.ai.OllamaRequest;
import com.ai_resume.ai_service.dto.ai.OllamaResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaClient {

    private final RestClient restClient;

    @Value("${ollama.chat-endpoint}")
    private String endpoint;

    public OllamaClient(RestClient restClient) {

        this.restClient = restClient;

    }


    public OllamaResponse chat(OllamaRequest request) {

        return restClient
                .post()
                .uri(endpoint)
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

    }

}