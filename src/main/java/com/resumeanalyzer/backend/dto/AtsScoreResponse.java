package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsScoreResponse {
    private Integer overallScore;
    private Integer formattingScore;
    private Integer keywordScore;
    private Integer grammarScore;
    private Integer projectsScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer skillsScore;
    private String strengths;
    private String weaknesses;
    private String suggestions;
}