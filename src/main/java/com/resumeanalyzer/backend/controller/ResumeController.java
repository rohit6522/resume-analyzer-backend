package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.AtsScoreResponse;
import com.resumeanalyzer.backend.dto.JobMatchRequest;
import com.resumeanalyzer.backend.dto.JobMatchResponse;
import com.resumeanalyzer.backend.dto.ParsedResumeResponse;
import com.resumeanalyzer.backend.dto.ResumeResponse;
import com.resumeanalyzer.backend.entity.AtsScore;
import com.resumeanalyzer.backend.entity.JobMatch;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.service.AtsService;
import com.resumeanalyzer.backend.service.JobMatchService;
import com.resumeanalyzer.backend.service.ReportService;
import com.resumeanalyzer.backend.service.ResumeParserService;
import com.resumeanalyzer.backend.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeParserService resumeParserService;
    private final AtsService atsService;
    private final JobMatchService jobMatchService;
    private final ReportService reportService;

    @PostMapping("/upload")
    public ResponseEntity<ResumeResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        ResumeResponse response = resumeService.uploadResume(authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getMyResumes(Authentication authentication) {
        return ResponseEntity.ok(resumeService.getUserResumes(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        resumeService.deleteResume(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/parsed-data")
    public ResponseEntity<ParsedResumeResponse> getParsedData(
            @PathVariable Long id, Authentication authentication) {

        resumeService.getOwnedResume(authentication.getName(), id);
        var parsedData = resumeParserService.getParsedDataByResumeId(id);

        ParsedResumeResponse response = ParsedResumeResponse.builder()
                .fullName(parsedData.getFullName())
                .email(parsedData.getEmail())
                .phone(parsedData.getPhone())
                .linkedin(parsedData.getLinkedin())
                .github(parsedData.getGithub())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate-ats-score")
    public ResponseEntity<AtsScoreResponse> generateAtsScore(
            @PathVariable Long id, Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        AtsScore score = atsService.generateAtsScore(resume);
        return ResponseEntity.ok(mapAtsToResponse(score));
    }

    @GetMapping("/{id}/ats-score")
    public ResponseEntity<AtsScoreResponse> getAtsScore(
            @PathVariable Long id, Authentication authentication) {

        resumeService.getOwnedResume(authentication.getName(), id);
        AtsScore score = atsService.getAtsScoreByResumeId(id);
        return ResponseEntity.ok(mapAtsToResponse(score));
    }

    @PostMapping("/{id}/match-job")
    public ResponseEntity<JobMatchResponse> matchJob(
            @PathVariable Long id,
            @Valid @RequestBody JobMatchRequest request,
            Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        JobMatch match = jobMatchService.matchResumeToJob(resume, request.getJobDescription());
        return ResponseEntity.ok(mapJobMatchToResponse(match));
    }

    @GetMapping("/{id}/job-matches")
    public ResponseEntity<List<JobMatchResponse>> getJobMatches(
            @PathVariable Long id, Authentication authentication) {

        resumeService.getOwnedResume(authentication.getName(), id);
        List<JobMatchResponse> matches = jobMatchService.getMatchesForResume(id)
                .stream()
                .map(this::mapJobMatchToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable Long id, Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        byte[] pdfBytes = reportService.generateReport(resume);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-report.pdf")
                .body(pdfBytes);
    }

    private AtsScoreResponse mapAtsToResponse(AtsScore score) {
        return AtsScoreResponse.builder()
                .overallScore(score.getOverallScore())
                .formattingScore(score.getFormattingScore())
                .keywordScore(score.getKeywordScore())
                .grammarScore(score.getGrammarScore())
                .projectsScore(score.getProjectsScore())
                .experienceScore(score.getExperienceScore())
                .educationScore(score.getEducationScore())
                .skillsScore(score.getSkillsScore())
                .strengths(score.getStrengths())
                .weaknesses(score.getWeaknesses())
                .suggestions(score.getSuggestions())
                .dateConsistencyNotes(score.getDateConsistencyNotes())
                .build();
    }

    private JobMatchResponse mapJobMatchToResponse(JobMatch match) {
        return JobMatchResponse.builder()
                .id(match.getId())
                .matchPercentage(match.getMatchPercentage())
                .matchedSkills(match.getMatchedSkills())
                .missingSkills(match.getMissingSkills())
                .recommendations(match.getRecommendations())
                .createdAt(match.getCreatedAt())
                .build();
    }
}