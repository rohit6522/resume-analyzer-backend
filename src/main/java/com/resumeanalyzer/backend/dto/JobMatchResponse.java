package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchResponse {
    private Long id;
    private Long resumeId;
    private String jobTitle;
    private String companyName;
    private Integer matchPercentage;
    private String matchedSkills;
    private String missingSkills;
    private String matchedTechnicalSkills;
    private String missingTechnicalSkills;
    private String matchedKeywords;
    private String missingKeywords;
    private String relevantExperience;
    private String experienceGaps;
    private String educationRequirements;
    private String recommendedSkillsToLearn;
    private String recommendations;
    private LocalDateTime createdAt;
}
