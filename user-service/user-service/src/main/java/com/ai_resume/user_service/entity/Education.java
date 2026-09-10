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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One college / university / bootcamp entry.
 *
 * Years rather than full dates: nobody puts "14 August 2021" for a degree on a
 * resume, and asking for a day people have to look up is friction for no gain.
 */
@Entity
@Table(name = "user_educations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id")
    private Long educationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    /** College or university name. */
    @Column(name = "institution", nullable = false, length = 200)
    private String institution;

    @Column(name = "degree", length = 160)
    private String degree;

    @Column(name = "field_of_study", length = 160)
    private String fieldOfStudy;

    @Column(name = "location", length = 160)
    private String location;

    @Column(name = "start_year")
    private Integer startYear;

    /** Null while still studying. */
    @Column(name = "end_year")
    private Integer endYear;

    /** Free text, so "8.6 CGPA", "First Class" and "3.9/4.0" all fit. */
    @Column(name = "grade", length = 60)
    private String grade;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
