package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapRequest {

    @NotBlank(message = "Career track is required")
    private String careerTrack; // FRONTEND, BACKEND, FULLSTACK, JAVA, PYTHON, DATA_ANALYST, DATA_SCIENTIST, AIML, DEVOPS

    private String targetCompanyOrLevel; // e.g. Junior, Mid, Senior, FAANG, Startup
}

