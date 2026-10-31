package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParsedResumeRequest {
    private String fullName;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
    private String portfolio;
    private String summary;
    private String technicalSkills;
    private String softSkills;
    private String skills;
    private String workExperience;
    private String education;
    private String projects;
    private String certifications;
    private String achievements;
}

