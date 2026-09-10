package com.ai_resume.ai_service.exception;


import java.time.LocalDateTime;

public record ErrorResponse(

        LocalDateTime timestamp,

        int status,

        String error,

        String message

) {
}