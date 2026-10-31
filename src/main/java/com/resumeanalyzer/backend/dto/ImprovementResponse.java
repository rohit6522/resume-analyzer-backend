package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImprovementResponse {
    private String originalText;
    private String improvedText;
    private String improvementType;
    private String changesSummary;
    private List<String> questionsForUser; // Details needed if metrics/tools are missing
    private List<String> powerVerbsUsed;
    private String rationale;
}

