package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.AiAnalysisRequest;
import com.ai_resume.resume_service.dto.AiAnalysisResponse;
import com.ai_resume.resume_service.dto.AnalysisResponse;
import com.ai_resume.resume_service.dto.AnalysisTriggerRequest;
import com.ai_resume.resume_service.dto.CloudinaryUploadResponse;
import com.ai_resume.resume_service.dto.JobDescriptionResponse;
import com.ai_resume.resume_service.dto.JobDescriptionUpdateRequest;
import com.ai_resume.resume_service.dto.LearningResourceResponse;
import com.ai_resume.resume_service.dto.MarkdownGenerationRequest;
import com.ai_resume.resume_service.dto.MarkdownResumeResponse;
import com.ai_resume.resume_service.dto.NotificationAnalysisCompletedRequest;
import com.ai_resume.resume_service.dto.ResumeResponse;
import com.ai_resume.resume_service.dto.ResumeUpdateRequest;
import com.ai_resume.resume_service.dto.ResumeVersionResponse;
import com.ai_resume.resume_service.entity.AnalysisStatus;
import com.ai_resume.resume_service.entity.JobDescription;
import com.ai_resume.resume_service.entity.JobDescriptionSource;
import com.ai_resume.resume_service.entity.Resume;
import com.ai_resume.resume_service.entity.ResumeAnalysis;
import com.ai_resume.resume_service.entity.ResumeVersion;
import com.ai_resume.resume_service.exception.ExternalServiceException;
import com.ai_resume.resume_service.exception.FileProcessingException;
import com.ai_resume.resume_service.exception.InvalidFileTypeException;
import com.ai_resume.resume_service.exception.ResourceNotFoundException;
import com.ai_resume.resume_service.repository.JobDescriptionRepository;
import com.ai_resume.resume_service.repository.ResumeAnalysisRepository;
import com.ai_resume.resume_service.repository.ResumeRepository;
import com.ai_resume.resume_service.repository.ResumeVersionRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns resumes, resume versions, job descriptions and analyses.
 *
 * <p>EVERY public method takes the caller's {@code userId} (supplied by the API
 * gateway as {@code X-User-Id}) and scopes its query by it. The absence of that
 * check previously let any authenticated user read or delete any other user's
 * resumes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    /** Characters of job-description text sent with each list row. */
    private static final int PREVIEW_LENGTH = 280;

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    private static final String RESUME_FOLDER = "resumes";
    private static final String JD_FOLDER = "job-descriptions";

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final LearningResourceService learningResourceService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final CloudinaryService cloudinaryService;
    private final DocumentTextExtractor documentTextExtractor;
    private final AiClientService aiClientService;
    private final NotificationClientService notificationClientService;

    // =====================================================================
    // Resumes
    // =====================================================================

    /** Creates a new resume with version 1. */
    @Transactional
    public ResumeResponse uploadResume(Long userId, MultipartFile file, String displayName) {
        validateFile(file, "resume");

        String resolvedDisplayName = (displayName == null || displayName.isBlank())
                ? file.getOriginalFilename()
                : displayName.trim();

        Resume resume = Resume.builder()
                .userId(userId)
                .displayName(resolvedDisplayName)
                .versions(new ArrayList<>())
                .build();

        resume.addVersion(buildVersion(file, 1));
        Resume saved = resumeRepository.save(resume);
        log.info("User {} created resume {} (v1)", userId, saved.getResumeId());
        return toResumeResponse(saved);
    }

    /**
     * Adds a new version to an existing resume. Analyses already recorded stay
     * bound to the version they ran against, which is what makes "ATS improvement
     * over time" meaningful.
     */
    @Transactional
    public ResumeVersionResponse addResumeVersion(Long userId, Long resumeId, MultipartFile file) {
        validateFile(file, "resume");
        Resume resume = findResumeOrThrow(userId, resumeId);

        ResumeVersion version = buildVersion(file, resume.nextVersionNumber());
        resume.addVersion(version);
        resumeRepository.save(resume);

        log.info("User {} added v{} to resume {}", userId, version.getVersionNumber(), resumeId);
        return toVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public Page<ResumeResponse> getResumes(Long userId, Pageable pageable) {
        return resumeRepository.findByUserId(userId, pageable).map(this::toResumeResponse);
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Long userId, Long resumeId) {
        return toResumeResponse(findResumeOrThrow(userId, resumeId));
    }

    @Transactional(readOnly = true)
    public List<ResumeVersionResponse> getResumeVersions(Long userId, Long resumeId) {
        // Ownership is enforced by the query itself.
        List<ResumeVersion> versions = resumeVersionRepository
                .findByResume_ResumeIdAndResume_UserIdOrderByVersionNumberAsc(resumeId, userId);
        if (versions.isEmpty() && resumeRepository.findByResumeIdAndUserId(resumeId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Resume not found with id: " + resumeId);
        }
        return versions.stream().map(this::toVersionResponse).toList();
    }

    @Transactional
    public ResumeResponse updateResumeMetadata(Long userId, Long resumeId, ResumeUpdateRequest request) {
        Resume resume = findResumeOrThrow(userId, resumeId);
        resume.setDisplayName(request.getDisplayName().trim());
        resume.setNotes(request.getNotes());
        return toResumeResponse(resumeRepository.save(resume));
    }

    /**
     * Deletes a resume, all of its versions and all of their Cloudinary assets.
     *
     * <p>Refused when analyses exist: analyses are permanent history that the
     * dashboard and trends features depend on, so we never cascade-delete them.
     * The old implementation silently wiped them.
     */
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {
        Resume resume = findResumeOrThrow(userId, resumeId);

        if (resumeAnalysisRepository.existsByResume_ResumeId(resumeId)) {
            throw new IllegalStateException(
                    "This resume has analyses attached and cannot be deleted, because analysis "
                            + "history must be preserved.");
        }

        List<String> publicIds = resume.getVersions().stream()
                .map(ResumeVersion::getCloudinaryPublicId)
                .toList();

        resumeRepository.delete(resume);
        // Delete remote assets only after the DB delete succeeds, so a Cloudinary
        // failure can never leave a row pointing at a file that no longer exists.
        publicIds.forEach(cloudinaryService::deleteByPublicId);
        log.info("User {} deleted resume {} and {} version file(s)", userId, resumeId, publicIds.size());
    }

    // =====================================================================
    // Job descriptions
    // =====================================================================

    @Transactional
    public JobDescriptionResponse uploadJobDescription(
            Long userId, MultipartFile file, String text, String company, String jobTitle) {
        if(company == null || company.isBlank()) {
            throw new IllegalArgumentException("Company is required for job description upload");
        }
        if(jobTitle == null || jobTitle.isBlank()) {
            throw new IllegalArgumentException("Job title is required for job description upload");
        }
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = text != null && !text.isBlank();

        if (!hasFile && !hasText) {
            throw new IllegalArgumentException(
                    "Either a file or text is required for job description upload");
        }
        if (hasFile && hasText) {
            throw new IllegalArgumentException(
                    "Provide either a file or text for a job description, not both");
        }

        JobDescription jobDescription;
        if (hasFile) {
            validateFile(file, "job description");
            byte[] bytes = readFileBytes(file);
            String extractedText = documentTextExtractor.extractText(
                    bytes, file.getOriginalFilename(), file.getContentType());
            CloudinaryUploadResponse upload =
                    cloudinaryService.upload(bytes, file.getOriginalFilename(), JD_FOLDER);

            jobDescription = JobDescription.builder()
                    .userId(userId)
                    .source(JobDescriptionSource.FILE)
                    .fileName(file.getOriginalFilename())
                    .contentType(resolveContentType(file))
                    .cloudinaryUrl(upload.getUrl())
                    .cloudinaryPublicId(upload.getPublicId())
                    .extractedText(extractedText)
                    .build();
        } else {
            String cleanedText = text.trim();
            String generatedName = "job-description-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            CloudinaryUploadResponse upload =
                    cloudinaryService.uploadText(cleanedText, generatedName, JD_FOLDER);

            jobDescription = JobDescription.builder()
                    .userId(userId)
                    .source(JobDescriptionSource.TEXT)
                    .fileName(generatedName + ".txt")
                    .contentType("text/plain")
                    .cloudinaryUrl(upload.getUrl())
                    .cloudinaryPublicId(upload.getPublicId())
                    .extractedText(cleanedText)
                    .build();
        }

        jobDescription.setCompany(trimToNull(company));
        jobDescription.setJobTitle(trimToNull(jobTitle));

        JobDescription saved = jobDescriptionRepository.save(jobDescription);
        log.info("User {} created job description {}", userId, saved.getJobDescriptionId());
        return toJobDescriptionResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<JobDescriptionResponse> getJobDescriptions(Long userId, Pageable pageable) {
        return jobDescriptionRepository.findByUserId(userId, pageable)
                .map(this::toJobDescriptionResponse);
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponse getJobDescriptionById(Long userId, Long jobDescriptionId) {
        return toJobDescriptionResponse(findJobDescriptionOrThrow(userId, jobDescriptionId), true);
    }

    /**
     * Renames / re-edits a job description.
     *
     * Null fields are left untouched so a rename can send just the title.
     * Replacement text is accepted only for TEXT-sourced entries: for an
     * uploaded file the stored text is what was parsed out of the document, and
     * letting the two drift apart would make analyses unreproducible.
     */
    /**
     * Resolves the skills this analysis flagged into curated resources.
     * Missing skills lead, since those are the actual blockers; suggested
     * skills follow as the next thing to pick up.
     */
    @Transactional(readOnly = true)
    public List<LearningResourceResponse> getLearningResourcesForAnalysis(
            Long userId, Long analysisId, int limit) {

        ResumeAnalysis analysis = resumeAnalysisRepository
                .findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found with id: " + analysisId));

        List<String> skills = new ArrayList<>();
        if (analysis.getMissingSkills() != null) skills.addAll(analysis.getMissingSkills());
        if (analysis.getSuggestedSkills() != null) skills.addAll(analysis.getSuggestedSkills());

        return learningResourceService.findForSkills(skills, Math.max(1, Math.min(limit, 20)));
    }

    @Transactional
    public JobDescriptionResponse updateJobDescription(
            Long userId, Long jobDescriptionId, JobDescriptionUpdateRequest request) {

        JobDescription jd = findJobDescriptionOrThrow(userId, jobDescriptionId);

        if (request.getCompany() != null) {
            jd.setCompany(blankToNull(request.getCompany()));
        }

        if (request.getJobTitle() != null) {
            jd.setJobTitle(blankToNull(request.getJobTitle()));
        }

        if (request.getText() != null) {
            if (jd.getSource() != JobDescriptionSource.TEXT) {
                throw new IllegalArgumentException(
                        "The text of an uploaded job description cannot be edited. "
                                + "Delete it and upload the corrected file instead.");
            }

            String text = request.getText().trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("Job description text cannot be empty.");
            }
            jd.setExtractedText(text);
        }

        return toJobDescriptionResponse(jobDescriptionRepository.save(jd), true);
    }

    @Transactional
    public void deleteJobDescription(Long userId, Long jobDescriptionId) {
        JobDescription jd = findJobDescriptionOrThrow(userId, jobDescriptionId);

        if (resumeAnalysisRepository.existsByJobDescription_JobDescriptionId(jobDescriptionId)) {
            throw new IllegalStateException(
                    "This job description has analyses attached and cannot be deleted, because "
                            + "analysis history must be preserved.");
        }

        String publicId = jd.getCloudinaryPublicId();
        jobDescriptionRepository.delete(jd);
        // Previously JD assets were never removed from Cloudinary at all.
        cloudinaryService.deleteByPublicId(publicId);
        log.info("User {} deleted job description {}", userId, jobDescriptionId);
    }

    // =====================================================================
    // Analyses
    // =====================================================================

    /**
     * Records an analysis and asks ai-service to fill it in.
     *
     * <p>The record is persisted BEFORE the remote call, so a failure still leaves
     * an auditable row (status FAILED) rather than a 502 and no trace. ai-service
     * is not implemented yet, so today this always ends up FAILED — by design, not
     * by accident.
     */
    @Transactional
    public AnalysisResponse triggerAnalysis(Long userId, AnalysisTriggerRequest request) {
        return triggerAnalysis(userId, request, null);
    }

    /**
     * Overload that also accepts the caller's email (forwarded by the API
     * gateway as {@code X-User-Email}) so an "analysis completed" email can be
     * sent once the analysis reaches COMPLETED. The email is best-effort: its
     * failure never affects the analysis result returned to the client.
     */
    @Transactional
    public AnalysisResponse triggerAnalysis(Long userId, AnalysisTriggerRequest request, String userEmail) {
        Resume resume = findResumeOrThrow(userId, request.getResumeId());
        JobDescription jobDescription =
                findJobDescriptionOrThrow(userId, request.getJobDescriptionId());
        ResumeVersion version = resolveVersion(userId, resume, request.getResumeVersionId());

        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .userId(userId)
                .resume(resume)
                .resumeVersion(version)
                .jobDescription(jobDescription)
                .status(AnalysisStatus.PENDING)
                .missingSkills(new ArrayList<>())
                .suggestedSkills(new ArrayList<>())
                .suggestions(new ArrayList<>())
                .build();
        analysis = resumeAnalysisRepository.save(analysis);

        AiAnalysisRequest aiRequest = AiAnalysisRequest.builder()
                .analysisId(analysis.getAnalysisId())
                .resumeText(version.getExtractedText())
                .jobDescriptionText(jobDescription.getExtractedText())
                .company(jobDescription.getCompany())
                .jobTitle(jobDescription.getJobTitle())
                .build();

        try {
            AiAnalysisResponse result = aiClientService.analyze(aiRequest);
            applyResult(analysis, result);
            log.info("Analysis {} completed for user {}", analysis.getAnalysisId(), userId);
        } catch (ExternalServiceException ex) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(ex.getMessage());
            analysis.setCompletedAt(LocalDateTime.now());
            log.warn("Analysis {} failed for user {}: {}",
                    analysis.getAnalysisId(), userId, ex.getMessage());
        }

        ResumeAnalysis saved = resumeAnalysisRepository.save(analysis);

        if (saved.getStatus() == AnalysisStatus.COMPLETED) {
            notifyAnalysisCompleted(saved, userEmail);
        }

        return toAnalysisResponse(saved);
    }

    /** Never allowed to throw - a notification failure must not affect the analysis result. */
    private void notifyAnalysisCompleted(ResumeAnalysis analysis, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            log.debug("No user email available for analysis {}; skipping notification", analysis.getAnalysisId());
            return;
        }
        try {
            JobDescription jd = analysis.getJobDescription();
            String jobDescriptionName = jd.getJobTitle() != null ? jd.getJobTitle() : jd.getFileName();
            String userName = userEmail.contains("@") ? userEmail.substring(0, userEmail.indexOf('@')) : userEmail;

            notificationClientService.sendAnalysisCompletedEmail(NotificationAnalysisCompletedRequest.builder()
                    .recipientEmail(userEmail)
                    .userName(userName)
                    .resumeName(analysis.getResume().getDisplayName())
                    .jobDescriptionName(jobDescriptionName)
                    .atsScore(analysis.getAtsScore())
                    .matchScore(analysis.getMatchScore())
                    .analysisId(analysis.getAnalysisId())
                    .overallSummary(analysis.getOverallSummary())
                    .matchingSkills(analysis.getMatchingSkills())
                    .missingSkills(analysis.getMissingSkills())
                    .suggestedSkills(analysis.getSuggestedSkills())
                    .strengths(analysis.getStrengths())
                    .weaknesses(analysis.getWeaknesses())
                    .suggestions(analysis.getSuggestions())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to trigger analysis-completed notification for analysis {}: {}",
                    analysis.getAnalysisId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AnalysisResponse> getAnalyses(
            Long userId, Long resumeId, Long resumeVersionId, Long jobDescriptionId, Pageable pageable) {

        Page<ResumeAnalysis> page;
        if (resumeVersionId != null) {
            page = resumeAnalysisRepository
                    .findByUserIdAndResumeVersion_ResumeVersionId(userId, resumeVersionId, pageable);
        } else if (resumeId != null) {
            page = resumeAnalysisRepository.findByUserIdAndResume_ResumeId(userId, resumeId, pageable);
        } else if (jobDescriptionId != null) {
            page = resumeAnalysisRepository
                    .findByUserIdAndJobDescription_JobDescriptionId(userId, jobDescriptionId, pageable);
        } else {
            page = resumeAnalysisRepository.findByUserId(userId, pageable);
        }
        return page.map(this::toAnalysisResponse);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysisById(Long userId, Long analysisId) {
        return toAnalysisResponse(resumeAnalysisRepository
                .findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found with id: " + analysisId)));
    }

    /**
     * Generates a JD-tailored Markdown version of the resume used in a completed
     * analysis, weaving in the missing/suggested skills that analysis found.
     * Result is cached on the analysis row so repeat calls don't re-hit the LLM
     * unless {@code forceRegenerate} is true.
     */
    @Transactional
    public MarkdownResumeResponse generateMarkdownResume(Long userId, Long analysisId, boolean forceRegenerate) {
        ResumeAnalysis analysis = resumeAnalysisRepository
                .findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found with id: " + analysisId));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Analysis " + analysisId + " is not COMPLETED (status: " + analysis.getStatus()
                            + "); cannot generate a tailored resume yet.");
        }

        if (!forceRegenerate && analysis.getGeneratedMarkdown() != null) {
            return MarkdownResumeResponse.builder()
                    .analysisId(analysisId)
                    .markdownCode(analysis.getGeneratedMarkdown())
                    .engineVersion(analysis.getEngineVersion())
                    .build();
        }

        JobDescription jobDescription = analysis.getJobDescription();
        MarkdownGenerationRequest request = MarkdownGenerationRequest.builder()
                .analysisId(analysisId)
                .resumeText(analysis.getResumeVersion().getExtractedText())
                .jobDescriptionText(jobDescription.getExtractedText())
                .company(jobDescription.getCompany())
                .jobTitle(jobDescription.getJobTitle())
                .missingSkills(analysis.getMissingSkills())
                .suggestedSkills(analysis.getSuggestedSkills())
                .build();

        MarkdownResumeResponse result = aiClientService.generateMarkdown(request);

        analysis.setGeneratedMarkdown(result.getMarkdownCode());
        resumeAnalysisRepository.save(analysis);

        log.info("Generated Markdown resume for analysis {} (user {})", analysisId, userId);
        return result;
    }

    /**
     * Read-only fetch of an already-generated tailored resume. Never calls
     * ai-service, so it is safe to invoke on every page load.
     */
    @Transactional(readOnly = true)
    public MarkdownResumeResponse getGeneratedMarkdownResume(Long userId, Long analysisId) {
        ResumeAnalysis analysis = resumeAnalysisRepository
                .findByAnalysisIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis not found with id: " + analysisId));

        String markdown = analysis.getGeneratedMarkdown();
        if (markdown == null || markdown.isBlank()) {
            throw new ResourceNotFoundException(
                    "No tailored resume has been generated for analysis " + analysisId);
        }

        return MarkdownResumeResponse.builder()
                .analysisId(analysisId)
                .markdownCode(markdown)
                .engineVersion(analysis.getEngineVersion())
                .build();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private ResumeVersion buildVersion(MultipartFile file, int versionNumber) {
        // Read the upload exactly once and share the bytes between the text
        // extractor and Cloudinary.
        byte[] fileBytes = readFileBytes(file);

        // Extract text BEFORE uploading, so an unreadable document fails fast
        // without leaving an orphaned Cloudinary asset behind.
        String extractedText = documentTextExtractor.extractText(
                fileBytes, file.getOriginalFilename(), file.getContentType());

        CloudinaryUploadResponse upload =
                cloudinaryService.upload(fileBytes, file.getOriginalFilename(), RESUME_FOLDER);

        return ResumeVersion.builder()
                .versionNumber(versionNumber)
                .fileName(file.getOriginalFilename())
                .contentType(resolveContentType(file))
                .fileSizeBytes(file.getSize())
                .cloudinaryUrl(upload.getUrl())
                .cloudinaryPublicId(upload.getPublicId())
                .extractedText(extractedText)
                .build();
    }

    private ResumeVersion resolveVersion(Long userId, Resume resume, Long requestedVersionId) {
        if (requestedVersionId == null) {
            ResumeVersion latest = resume.latestVersion();
            if (latest == null) {
                throw new ResourceNotFoundException(
                        "Resume " + resume.getResumeId() + " has no uploaded version to analyse");
            }
            return latest;
        }
        ResumeVersion version = resumeVersionRepository
                .findByResumeVersionIdAndResume_UserId(requestedVersionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume version not found with id: " + requestedVersionId));
        if (!version.getResume().getResumeId().equals(resume.getResumeId())) {
            throw new IllegalArgumentException(
                    "Resume version " + requestedVersionId + " does not belong to resume "
                            + resume.getResumeId());
        }
        return version;
    }

    private void applyResult(ResumeAnalysis analysis, AiAnalysisResponse result) {
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setAtsScore(clampScore(result.getAtsScore()));
        analysis.setMatchScore(clampScore(result.getMatchScore()));
        analysis.setOverallSummary(result.getSummary());
        analysis.setMatchingSkills(safeList(result.getMatchingSkills()));
        analysis.setMissingSkills(safeList(result.getMissingSkills()));
        analysis.setStrengths(safeList(result.getStrengths()));
        analysis.setWeaknesses(safeList(result.getWeaknesses()));
        analysis.setSuggestedSkills(safeList(result.getSuggestedSkills()));
        analysis.setSuggestions(safeList(result.getSuggestions()));
        analysis.setEngineVersion(result.getEngineVersion());
        analysis.setCompletedAt(LocalDateTime.now());
    }

    private Integer clampScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(100, score));
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Resume findResumeOrThrow(Long userId, Long resumeId) {
        return resumeRepository.findByResumeIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
    }

    private JobDescription findJobDescriptionOrThrow(Long userId, Long jobDescriptionId) {
        return jobDescriptionRepository.findByJobDescriptionIdAndUserId(jobDescriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job description not found with id: " + jobDescriptionId));
    }

    private void validateFile(MultipartFile file, String resourceName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A " + resourceName + " file is required");
        }
        String contentType = resolveContentType(file);
        String fileName = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase();

        boolean validByType = SUPPORTED_CONTENT_TYPES.contains(contentType);
        boolean validByExtension = SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
        if (!validByType && !validByExtension) {
            throw new InvalidFileTypeException("Only PDF, DOC, and DOCX files are supported");
        }
    }

    private String resolveContentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            log.error("Failed to read bytes from uploaded file: {}", file.getOriginalFilename(), ex);
            throw new FileProcessingException(
                    "Failed to read uploaded file: " + file.getOriginalFilename(), ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    // ----------------------------- mappers -------------------------------

    private ResumeResponse toResumeResponse(Resume resume) {
        ResumeVersion latest = resume.latestVersion();
        return ResumeResponse.builder()
                .resumeId(resume.getResumeId())
                .displayName(resume.getDisplayName())
                .notes(resume.getNotes())
                .fileName(latest == null ? null : latest.getFileName())
                .contentType(latest == null ? null : latest.getContentType())
                .cloudinaryUrl(latest == null ? null : latest.getCloudinaryUrl())
                .latestVersionNumber(latest == null ? null : latest.getVersionNumber())
                .latestVersionId(latest == null ? null : latest.getResumeVersionId())
                .totalVersions(resume.getVersions().size())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    private ResumeVersionResponse toVersionResponse(ResumeVersion version) {
        return ResumeVersionResponse.builder()
                .resumeVersionId(version.getResumeVersionId())
                .resumeId(version.getResume().getResumeId())
                .versionNumber(version.getVersionNumber())
                .fileName(version.getFileName())
                .contentType(version.getContentType())
                .fileSizeBytes(version.getFileSizeBytes())
                .cloudinaryUrl(version.getCloudinaryUrl())
                .createdAt(version.getCreatedAt())
                .build();
    }

    /** List projection: preview only, so a page of JDs stays small. */
    private JobDescriptionResponse toJobDescriptionResponse(JobDescription jd) {
        return toJobDescriptionResponse(jd, false);
    }

    /**
     * @param includeFullText true for the single-item endpoint, where the user
     *                        actually wants to read the job description.
     */
    private JobDescriptionResponse toJobDescriptionResponse(
            JobDescription jd, boolean includeFullText) {

        String text = jd.getExtractedText();

        return JobDescriptionResponse.builder()
                .jobDescriptionId(jd.getJobDescriptionId())
                .company(jd.getCompany())
                .jobTitle(jd.getJobTitle())
                .source(jd.getSource())
                .fileName(jd.getFileName())
                .contentType(jd.getContentType())
                .cloudinaryUrl(jd.getCloudinaryUrl())
                .createdAt(jd.getCreatedAt())
                .extractedText(includeFullText ? text : null)
                .textPreview(buildPreview(text))
                .textLength(text == null ? 0 : text.length())
                .build();
    }

    /**
     * First ~280 characters, cut on a word boundary so the preview does not end
     * mid-word. Newlines are collapsed because the list renders a single line.
     */
    /** Treats "" and "   " as an explicit clear. */
    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildPreview(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String flattened = text.replaceAll("\\s+", " ").trim();
        if (flattened.length() <= PREVIEW_LENGTH) {
            return flattened;
        }

        String cut = flattened.substring(0, PREVIEW_LENGTH);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > PREVIEW_LENGTH / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "\u2026";
    }

    private AnalysisResponse toAnalysisResponse(ResumeAnalysis analysis) {
        JobDescription jd = analysis.getJobDescription();
        ResumeVersion version = analysis.getResumeVersion();
        return AnalysisResponse.builder()
                .analysisId(analysis.getAnalysisId())
                .resumeId(analysis.getResume().getResumeId())
                .resumeDisplayName(analysis.getResume().getDisplayName())
                .resumeVersionId(version.getResumeVersionId())
                .resumeVersionNumber(version.getVersionNumber())
                .jobDescriptionId(jd.getJobDescriptionId())
                .company(jd.getCompany())
                .jobTitle(jd.getJobTitle())
                .status(analysis.getStatus())
                .atsScore(analysis.getAtsScore())
                .matchScore(analysis.getMatchScore())
                .overallSummary(analysis.getOverallSummary())
                .matchingSkills(safeList(analysis.getMatchingSkills()))
                .missingSkills(safeList(analysis.getMissingSkills()))
                .strengths(safeList(analysis.getStrengths()))
                .weaknesses(safeList(analysis.getWeaknesses()))
                .suggestedSkills(safeList(analysis.getSuggestedSkills()))
                .suggestions(safeList(analysis.getSuggestions()))
                .errorMessage(analysis.getErrorMessage())
                .engineVersion(analysis.getEngineVersion())
                .createdAt(analysis.getCreatedAt())
                .completedAt(analysis.getCompletedAt())
                .build();
    }
}
