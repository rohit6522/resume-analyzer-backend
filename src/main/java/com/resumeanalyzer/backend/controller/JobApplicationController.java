package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.JobApplicationDto;
import com.resumeanalyzer.backend.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<JobApplicationDto>> getApplications(Authentication authentication) {
        return ResponseEntity.ok(applicationService.getUserApplications(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<JobApplicationDto> createApplication(
            @Valid @RequestBody JobApplicationDto dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(authentication.getName(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobApplicationDto> updateApplication(
            @PathVariable Long id,
            @RequestBody JobApplicationDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(applicationService.updateApplication(authentication.getName(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id, Authentication authentication) {
        applicationService.deleteApplication(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

