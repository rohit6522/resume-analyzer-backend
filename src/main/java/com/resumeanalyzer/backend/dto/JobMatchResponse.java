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
    private Integer matchPercentage;
    private String matchedSkills;
    private String missingSkills;
    private String recommendations;
    private LocalDateTime createdAt;
}