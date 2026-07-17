package com.resumeanalyzer.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedResumeResponse {
    private String fullName;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
}