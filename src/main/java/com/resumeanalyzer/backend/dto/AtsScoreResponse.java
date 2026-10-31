package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsScoreResponse {
    private Integer overallScore;
    
    // 7 Core transparent evaluation criteria
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer projectsScore;
    private Integer educationScore;
    private Integer formattingScore;
    private Integer contentQualityScore;
    private Integer atsCompatibilityScore;

    // Sub-scores
    private Integer keywordScore;
    private Integer grammarScore;
    private Integer actionVerbsScore;
    private Integer quantifiableMetricsScore;

    // Explanations and feedback
    private String strengths;
    private String weaknesses;
    private String suggestions;
    private String actionVerbsFound;
    private String actionVerbsMissing;
    private String quantifiableMetricsNotes;
    private String missingSections;
    private String dateConsistencyNotes;
    private String evaluationDisclaimer;
    private String sectionBreakdownJson;
}
