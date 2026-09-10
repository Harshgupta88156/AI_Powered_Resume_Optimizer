package com.ai_resume.history_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "job_descriptions")
@Getter
@Setter
public class JobDescription {

    @Id
    @Column(name = "job_description_id")
    private Long jobDescriptionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company")
    private String company;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

