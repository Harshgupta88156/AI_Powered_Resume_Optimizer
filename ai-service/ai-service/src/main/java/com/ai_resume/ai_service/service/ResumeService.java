package com.ai_resume.ai_service.service;

import com.ai_resume.ai_service.dto.request.AiAnalysisRequest;
import com.ai_resume.ai_service.dto.request.MarkdownGenerationRequest;
import com.ai_resume.ai_service.dto.response.AiAnalysisResponse;
import com.ai_resume.ai_service.dto.response.MarkdownGenerationResponse;

public interface ResumeService {

    AiAnalysisResponse analyzeResume(AiAnalysisRequest request);

    MarkdownGenerationResponse generateMarkdownResume(MarkdownGenerationRequest request);

}
