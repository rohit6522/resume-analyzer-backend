package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImprovementRequest {

    @NotBlank(message = "Text to improve is required")
    private String text;

    private String targetRole;

    private String improvementType; // SUMMARY, BULLETS, ACTION_VERBS, QUANTIFY, GRAMMAR, TAILOR

    private String context; // e.g. company, project name, or user guidance
}

