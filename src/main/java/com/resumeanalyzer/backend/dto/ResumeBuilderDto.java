package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeBuilderDto {
    private Long id;
    private Long resumeId;

    @NotBlank(message = "Resume title is required")
    private String title;

    private String templateName; // MODERN_PROFESSIONAL, MINIMAL_ATS, SOFTWARE_ENGINEER, FRESH_GRADUATE, EXPERIENCED_DEVELOPER

    private String profileDataJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

