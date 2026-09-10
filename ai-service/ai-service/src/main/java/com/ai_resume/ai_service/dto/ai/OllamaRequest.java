package com.ai_resume.ai_service.dto.ai;

import java.util.List;

public record OllamaRequest(

        String model,

        List<Message> messages,

        boolean stream

) {
}