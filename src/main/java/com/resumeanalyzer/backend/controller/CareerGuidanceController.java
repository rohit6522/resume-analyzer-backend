package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.InterviewPrepRequest;
import com.resumeanalyzer.backend.dto.InterviewPrepResponse;
import com.resumeanalyzer.backend.dto.SkillGapRequest;
import com.resumeanalyzer.backend.dto.SkillGapResponse;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.service.CareerGuidanceService;
import com.resumeanalyzer.backend.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerGuidanceController {

    private final ResumeService resumeService;
    private final CareerGuidanceService careerGuidanceService;

    @PostMapping("/resumes/{id}/skill-gap")
    public ResponseEntity<SkillGapResponse> getSkillGap(
            @PathVariable Long id,
            @Valid @RequestBody SkillGapRequest request,
            Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        return ResponseEntity.ok(careerGuidanceService.analyzeSkillGap(resume, request));
    }

    @PostMapping("/resumes/{id}/interview-prep")
    public ResponseEntity<InterviewPrepResponse> getInterviewPrep(
            @PathVariable Long id,
            @Valid @RequestBody InterviewPrepRequest request,
            Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        return ResponseEntity.ok(careerGuidanceService.generateInterviewQuestions(resume, request));
    }
}

