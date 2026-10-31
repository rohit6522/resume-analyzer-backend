package com.resumeanalyzer.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    private String jobTitle;
    private String companyName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String jobDescription;

    private Integer matchPercentage;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills; // legacy comma-separated

    @Column(columnDefinition = "TEXT")
    private String missingSkills; // legacy comma-separated

    @Column(columnDefinition = "TEXT")
    private String matchedTechnicalSkills;

    @Column(columnDefinition = "TEXT")
    private String missingTechnicalSkills;

    @Column(columnDefinition = "TEXT")
    private String matchedKeywords;

    @Column(columnDefinition = "TEXT")
    private String missingKeywords;

    @Column(columnDefinition = "TEXT")
    private String relevantExperience;

    @Column(columnDefinition = "TEXT")
    private String experienceGaps;

    @Column(columnDefinition = "TEXT")
    private String educationRequirements;

    @Column(columnDefinition = "TEXT")
    private String recommendedSkillsToLearn;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
