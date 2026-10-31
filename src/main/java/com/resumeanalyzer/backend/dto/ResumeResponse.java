package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {
    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private boolean active;
    private String versionLabel;
    private String targetRole;
    private LocalDateTime uploadedAt;
}
