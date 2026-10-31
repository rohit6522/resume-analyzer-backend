package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepRequest {
    private String targetJobTitle;
    private String jobDescription;
    private String difficultyLevel; // JUNIOR, MID, SENIOR
    private String focusArea; // ALL, TECHNICAL, PROJECT_DEEP_DIVE, SYSTEM_DESIGN, BEHAVIORAL, HR
}

