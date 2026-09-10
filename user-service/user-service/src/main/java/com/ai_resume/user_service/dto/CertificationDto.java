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
public class CertificationDto {

    private Long certificationId;

    @NotBlank(message = "Certificate name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 200)
    private String issuingOrganization;

    @Size(max = 160)
    private String credentialId;

    @Size(max = 300)
    private String credentialUrl;

    private LocalDate issueDate;
    private LocalDate expiryDate;
}
