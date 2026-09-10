package com.ai_resume.ai_service.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaResponse(

        String model,

        String created_at,

        Message message,

        boolean done

) {
}