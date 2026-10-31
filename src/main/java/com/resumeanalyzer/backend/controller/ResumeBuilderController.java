package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.ResumeBuilderDto;
import com.resumeanalyzer.backend.service.ResumeBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume-builder")
@RequiredArgsConstructor
public class ResumeBuilderController {

    private final ResumeBuilderService builderService;

    @GetMapping
    public ResponseEntity<List<ResumeBuilderDto>> getMyProfiles(Authentication authentication) {
        return ResponseEntity.ok(builderService.getUserProfiles(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeBuilderDto> getProfileById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(builderService.getProfileById(authentication.getName(), id));
    }

    @PostMapping
    public ResponseEntity<ResumeBuilderDto> createProfile(
            @Valid @RequestBody ResumeBuilderDto dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(builderService.createProfile(authentication.getName(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeBuilderDto> updateProfile(
            @PathVariable Long id,
            @RequestBody ResumeBuilderDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(builderService.updateProfile(authentication.getName(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id, Authentication authentication) {
        builderService.deleteProfile(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import/{resumeId}")
    public ResponseEntity<ResumeBuilderDto> importFromResume(
            @PathVariable Long resumeId,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(builderService.importFromResume(authentication.getName(), resumeId));
    }
}

