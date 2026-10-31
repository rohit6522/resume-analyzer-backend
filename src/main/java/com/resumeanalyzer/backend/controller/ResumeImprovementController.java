package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.ImprovementRequest;
import com.resumeanalyzer.backend.dto.ImprovementResponse;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.service.ResumeImprovementService;
import com.resumeanalyzer.backend.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resumes/{id}/improvements")
@RequiredArgsConstructor
public class ResumeImprovementController {

    private final ResumeService resumeService;
    private final ResumeImprovementService improvementService;

    @PostMapping
    public ResponseEntity<ImprovementResponse> improveText(
            @PathVariable Long id,
            @Valid @RequestBody ImprovementRequest request,
            Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        return ResponseEntity.ok(improvementService.improve(resume, request));
    }
}

