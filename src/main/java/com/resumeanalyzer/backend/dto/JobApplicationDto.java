package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationDto {
    private Long id;
    private Long resumeId;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private String status; // WISHLIST, APPLIED, INTERVIEWING, OFFER, REJECTED
    private String salaryRange;
    private String jobUrl;
    private Integer matchScore;
    private String notes;
    private LocalDate appliedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

