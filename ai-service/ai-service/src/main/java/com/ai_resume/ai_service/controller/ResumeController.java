package com.ai_resume.ai_service.controller;

import com.ai_resume.ai_service.dto.request.AiAnalysisRequest;
import com.ai_resume.ai_service.dto.request.MarkdownGenerationRequest;
import com.ai_resume.ai_service.dto.response.AiAnalysisResponse;
import com.ai_resume.ai_service.dto.response.MarkdownGenerationResponse;
import com.ai_resume.ai_service.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzeResume(@Valid @RequestBody AiAnalysisRequest request) {
        AiAnalysisResponse response = resumeService.analyzeResume(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-markdown")
    public ResponseEntity<MarkdownGenerationResponse> generateMarkdownResume(
            @Valid @RequestBody MarkdownGenerationRequest request) {
        MarkdownGenerationResponse response = resumeService.generateMarkdownResume(request);
        return ResponseEntity.ok(response);
    }

}

