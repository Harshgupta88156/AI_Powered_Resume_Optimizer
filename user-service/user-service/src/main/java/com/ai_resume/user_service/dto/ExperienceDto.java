package com.ai_resume.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDto {

    private Long experienceId;

    @NotBlank(message = "Job title is required")
    @Size(max = 160)
    private String jobTitle;

    @NotBlank(message = "Company is required")
    @Size(max = 160)
    private String company;

    @Size(max = 160)
    private String location;

    @Size(max = 60)
    private String employmentType;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentRole;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;
}
