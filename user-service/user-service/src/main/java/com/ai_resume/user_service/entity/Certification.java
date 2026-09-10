package com.ai_resume.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A certificate or credential the user holds. */
@Entity
@Table(name = "user_certifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_id")
    private Long certificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Awarding body, e.g. "Amazon Web Services". */
    @Column(name = "issuing_organization", length = 200)
    private String issuingOrganization;

    @Column(name = "credential_id", length = 160)
    private String credentialId;

    @Column(name = "credential_url", length = 300)
    private String credentialUrl;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /** Null means the credential does not expire. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
