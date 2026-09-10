package com.ai_resume.user_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {

    private Long educationId;

    @NotBlank(message = "Institution is required")
    @Size(max = 200)
    private String institution;

    @Size(max = 160)
    private String degree;

    @Size(max = 160)
    private String fieldOfStudy;

    @Size(max = 160)
    private String location;

    @Min(value = 1900, message = "Start year looks wrong")
    @Max(value = 2100, message = "Start year looks wrong")
    private Integer startYear;

    @Min(value = 1900, message = "End year looks wrong")
    @Max(value = 2100, message = "End year looks wrong")
    private Integer endYear;

    @Size(max = 60)
    private String grade;

    @Size(max = 4000)
    private String description;
}
