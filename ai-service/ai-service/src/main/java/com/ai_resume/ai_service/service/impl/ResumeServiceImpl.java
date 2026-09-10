package com.ai_resume.ai_service.service.impl;

import com.ai_resume.ai_service.dto.request.AiAnalysisRequest;
import com.ai_resume.ai_service.dto.request.MarkdownGenerationRequest;
import com.ai_resume.ai_service.dto.response.AiAnalysisResponse;
import com.ai_resume.ai_service.dto.response.MarkdownGenerationResponse;
import com.ai_resume.ai_service.service.AiService;
import com.ai_resume.ai_service.service.ResumeService;
import org.springframework.stereotype.Service;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final AiService aiService;

    public ResumeServiceImpl(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public AiAnalysisResponse analyzeResume(AiAnalysisRequest request) {
        return aiService.analyzeResume(request);
    }

    @Override
    public MarkdownGenerationResponse generateMarkdownResume(MarkdownGenerationRequest request) {
        return aiService.generateMarkdownResume(request);
    }
}