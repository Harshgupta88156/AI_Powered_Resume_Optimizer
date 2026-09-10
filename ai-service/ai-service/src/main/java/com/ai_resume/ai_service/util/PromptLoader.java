package com.ai_resume.ai_service.util;


import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    public String loadPrompt(String fileName) {

        try {

            ClassPathResource resource =
                    new ClassPathResource("prompts/" + fileName);

            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException ex) {

            throw new RuntimeException(
                    "Unable to load prompt : " + fileName,
                    ex
            );

        }

    }

}