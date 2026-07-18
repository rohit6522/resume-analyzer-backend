package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchRequest {

    @NotBlank(message = "Job description is required")
    private String jobDescription;
}