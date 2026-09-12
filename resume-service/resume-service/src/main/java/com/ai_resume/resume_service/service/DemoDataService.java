package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.DemoDataResponse;
import com.ai_resume.resume_service.entity.AnalysisStatus;
import com.ai_resume.resume_service.entity.JobDescription;
import com.ai_resume.resume_service.entity.JobDescriptionSource;
import com.ai_resume.resume_service.entity.Resume;
import com.ai_resume.resume_service.entity.ResumeAnalysis;
import com.ai_resume.resume_service.entity.ResumeVersion;
import com.ai_resume.resume_service.repository.JobDescriptionRepository;
import com.ai_resume.resume_service.repository.ResumeAnalysisRepository;
import com.ai_resume.resume_service.repository.ResumeRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoDataService {

    private static final String DEMO_PREFIX = "Demo data - ";

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    @Value("${demo.data.enabled:false}")
    private boolean enabled;

    @Transactional
    public DemoDataResponse seedForUser(Long userId) {
        if (!enabled) {
            throw new IllegalStateException(
                    "Demo data is disabled. Set DEMO_DATA_ENABLED=true on resume-service first."
            );
        }

        if (resumeRepository.findByUserIdAndDisplayName(
                userId, DEMO_PREFIX + "Java Backend Resume").isPresent()) {
            return new DemoDataResponse(0, 0, 0, true);
        }

        List<Resume> resumes = createResumes(userId);
        List<JobDescription> jobDescriptions = createJobDescriptions(userId);
        createAnalyses(userId, resumes, jobDescriptions);

        log.info(
                "Seeded demo data for user {}: {} resumes, {} job descriptions, 6 analyses",
                userId,
                resumes.size(),
                jobDescriptions.size()
        );

        return new DemoDataResponse(resumes.size(), jobDescriptions.size(), 6, false);
    }

    private List<Resume> createResumes(Long userId) {
        Resume backend = createResume(
                userId,
                "Java Backend Resume",
                "Java, Spring Boot, REST APIs, PostgreSQL, Docker, Kafka, Git"
        );
        Resume fullStack = createResume(
                userId,
                "Full Stack Resume",
                "Java, Spring Boot, Angular, TypeScript, PostgreSQL, REST APIs, AWS"
        );
        Resume data = createResume(
                userId,
                "Data Engineering Resume",
                "Python, SQL, AWS, Airflow, Spark, PostgreSQL, Docker"
        );
        return List.of(backend, fullStack, data);
    }

    private Resume createResume(Long userId, String name, String skills) {
        Resume resume = Resume.builder()
                .userId(userId)
                .displayName(DEMO_PREFIX + name)
                .notes("Generated demo resume for showcasing the dashboard.")
                .versions(new ArrayList<>())
                .build();

        ResumeVersion version = ResumeVersion.builder()
                .versionNumber(1)
                .fileName(name.replace(' ', '-') + ".pdf")
                .contentType("application/pdf")
                .fileSizeBytes(24576L)
                .cloudinaryUrl("https://demo.invalid/resumes/" + name.replace(' ', '-'))
                .cloudinaryPublicId("demo/resumes/" + name.replace(' ', '-'))
                .extractedText(
                        "Demo candidate with professional experience in " + skills
                                + ". Built production services, automated deployments, "
                                + "and collaborated with cross-functional teams."
                )
                .build();
        resume.addVersion(version);
        return resumeRepository.save(resume);
    }

    private List<JobDescription> createJobDescriptions(Long userId) {
        return List.of(
                createJobDescription(
                        userId,
                        "Acme Technologies",
                        "Backend Java Engineer",
                        "Java, Spring Boot, REST APIs, PostgreSQL, Docker, Kafka, and testing."
                ),
                createJobDescription(
                        userId,
                        "Northstar Labs",
                        "Full Stack Developer",
                        "Angular, TypeScript, Java, Spring Boot, REST APIs, PostgreSQL, and AWS."
                ),
                createJobDescription(
                        userId,
                        "Data Insights Co.",
                        "Data Engineer",
                        "Python, SQL, AWS, Airflow, Spark, Docker, and data pipeline design."
                ),
                createJobDescription(
                        userId,
                        "CloudScale",
                        "Platform Engineer",
                        "Kubernetes, Docker, AWS, Terraform, observability, and CI/CD."
                )
        );
    }

    private JobDescription createJobDescription(
            Long userId,
            String company,
            String jobTitle,
            String requirements
    ) {
        return jobDescriptionRepository.save(JobDescription.builder()
                .userId(userId)
                .company(company)
                .jobTitle(jobTitle)
                .source(JobDescriptionSource.TEXT)
                .fileName("demo-" + jobTitle.replace(' ', '-') + ".txt")
                .contentType("text/plain")
                .cloudinaryUrl("https://demo.invalid/job-descriptions/" + jobTitle.replace(' ', '-'))
                .cloudinaryPublicId("demo/job-descriptions/" + jobTitle.replace(' ', '-'))
                .extractedText(
                        "We are hiring a " + jobTitle + ". Required skills: " + requirements
                                + " The role values communication, ownership, and measurable delivery."
                )
                .build());
    }

    private void createAnalyses(
            Long userId,
            List<Resume> resumes,
            List<JobDescription> jobDescriptions
    ) {
        saveAnalysis(
                userId, resumes.get(0), jobDescriptions.get(0), 88, 91,
                List.of("Java", "Spring Boot", "PostgreSQL"),
                List.of("Kafka", "Docker"),
                "Strong backend fit with relevant Java and Spring Boot experience."
        );
        saveAnalysis(
                userId, resumes.get(0), jobDescriptions.get(1), 83, 72,
                List.of("Java", "Spring Boot", "REST APIs"),
                List.of("Angular", "TypeScript"),
                "Good backend foundation with a few frontend skills to strengthen."
        );
        saveAnalysis(
                userId, resumes.get(1), jobDescriptions.get(1), 86, 89,
                List.of("Angular", "TypeScript", "Java", "Spring Boot"),
                List.of("AWS"),
                "Very strong full-stack alignment for this role."
        );
        saveAnalysis(
                userId, resumes.get(2), jobDescriptions.get(2), 81, 87,
                List.of("Python", "SQL", "AWS", "Docker"),
                List.of("Airflow", "Spark"),
                "Strong data engineering foundation with pipeline tooling gaps."
        );
        saveAnalysis(
                userId, resumes.get(0), jobDescriptions.get(3), 76, 61,
                List.of("Docker", "AWS"),
                List.of("Kubernetes", "Terraform", "Observability"),
                "Some platform overlap, but the resume needs stronger infrastructure evidence."
        );
        saveAnalysis(
                userId, resumes.get(2), jobDescriptions.get(3), 74, 58,
                List.of("AWS", "Docker"),
                List.of("Kubernetes", "Terraform", "CI/CD"),
                "Transferable cloud experience is present, but platform requirements are incomplete."
        );
    }

    private void saveAnalysis(
            Long userId,
            Resume resume,
            JobDescription jobDescription,
            int atsScore,
            int matchScore,
            List<String> matchingSkills,
            List<String> missingSkills,
            String summary
    ) {
        ResumeVersion version = resume.latestVersion();
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .userId(userId)
                .resume(resume)
                .resumeVersion(version)
                .jobDescription(jobDescription)
                .status(AnalysisStatus.COMPLETED)
                .atsScore(atsScore)
                .matchScore(matchScore)
                .overallSummary(summary)
                .matchingSkills(new ArrayList<>(matchingSkills))
                .missingSkills(new ArrayList<>(missingSkills))
                .strengths(new ArrayList<>(matchingSkills))
                .weaknesses(new ArrayList<>(missingSkills))
                .suggestedSkills(new ArrayList<>(missingSkills))
                .suggestions(new ArrayList<>(List.of(
                        "Add measurable outcomes to the most relevant project bullets.",
                        "Move evidence for the missing skills closer to the top of the resume."
                )))
                .rawResult("{\"demo\":true}")
                .engineVersion("demo-seed/v1")
                .build();
        analysis.setCompletedAt(java.time.LocalDateTime.now());
        resumeAnalysisRepository.save(analysis);
    }
}
