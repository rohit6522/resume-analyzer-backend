package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.*;
import com.resumeanalyzer.backend.entity.AtsScore;
import com.resumeanalyzer.backend.entity.JobMatch;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
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
            @RequestParam(value = "versionLabel", required = false) String versionLabel,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            Authentication authentication) {

        ResumeResponse response = resumeService.uploadResume(authentication.getName(), file, versionLabel, targetRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getMyResumes(Authentication authentication) {
        return ResponseEntity.ok(resumeService.getUserResumes(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable Long id, Authentication authentication) {
        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        return ResponseEntity.ok(resumeService.mapToResponse(resume));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResumeMeta(
            @PathVariable Long id,
            @RequestParam(value = "versionLabel", required = false) String versionLabel,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            Authentication authentication) {
        return ResponseEntity.ok(resumeService.updateResumeMeta(authentication.getName(), id, versionLabel, targetRole));
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
        ParsedResumeData data = resumeParserService.getParsedDataByResumeId(id);
        return ResponseEntity.ok(mapParsedDataToResponse(data));
    }

    @PutMapping("/{id}/parsed-data")
    public ResponseEntity<ParsedResumeResponse> updateParsedData(
            @PathVariable Long id,
            @RequestBody UpdateParsedResumeRequest request,
            Authentication authentication) {

        resumeService.getOwnedResume(authentication.getName(), id);
        ParsedResumeData updated = resumeParserService.updateParsedData(id, request);
        return ResponseEntity.ok(mapParsedDataToResponse(updated));
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
        JobMatch match = jobMatchService.matchResumeToJob(resume, request.getJobDescription(), request.getJobTitle(), request.getCompanyName());
        return ResponseEntity.ok(jobMatchService.mapJobMatchToResponse(match));
    }

    @PostMapping(value = "/{id}/match-job-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobMatchResponse> matchJobFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobTitle", required = false) String jobTitle,
            @RequestParam(value = "companyName", required = false) String companyName,
            Authentication authentication) {

        Resume resume = resumeService.getOwnedResume(authentication.getName(), id);
        JobMatch match = jobMatchService.matchResumeToJobFile(resume, file, jobTitle, companyName);
        return ResponseEntity.ok(jobMatchService.mapJobMatchToResponse(match));
    }

    @GetMapping("/{id}/job-matches")
    public ResponseEntity<List<JobMatchResponse>> getJobMatches(
            @PathVariable Long id, Authentication authentication) {

        resumeService.getOwnedResume(authentication.getName(), id);
        List<JobMatchResponse> matches = jobMatchService.getMatchesForResume(id)
                .stream()
                .map(jobMatchService::mapJobMatchToResponse)
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

    private ParsedResumeResponse mapParsedDataToResponse(ParsedResumeData data) {
        return ParsedResumeResponse.builder()
                .id(data.getId())
                .resumeId(data.getResume().getId())
                .fullName(data.getFullName())
                .email(data.getEmail())
                .phone(data.getPhone())
                .linkedin(data.getLinkedin())
                .github(data.getGithub())
                .portfolio(data.getPortfolio())
                .summary(data.getSummary())
                .skills(data.getSkills())
                .technicalSkills(data.getTechnicalSkills())
                .softSkills(data.getSoftSkills())
                .workExperience(data.getWorkExperience())
                .education(data.getEducation())
                .projects(data.getProjects())
                .certifications(data.getCertifications())
                .achievements(data.getAchievements())
                .rawText(data.getRawText())
                .build();
    }

    private AtsScoreResponse mapAtsToResponse(AtsScore score) {
        return AtsScoreResponse.builder()
                .overallScore(score.getOverallScore())
                .skillsScore(score.getSkillsScore())
                .experienceScore(score.getExperienceScore())
                .projectsScore(score.getProjectsScore())
                .educationScore(score.getEducationScore())
                .formattingScore(score.getFormattingScore())
                .contentQualityScore(score.getContentQualityScore())
                .atsCompatibilityScore(score.getAtsCompatibilityScore())
                .keywordScore(score.getKeywordScore())
                .grammarScore(score.getGrammarScore())
                .actionVerbsScore(score.getActionVerbsScore())
                .quantifiableMetricsScore(score.getQuantifiableMetricsScore())
                .strengths(score.getStrengths())
                .weaknesses(score.getWeaknesses())
                .suggestions(score.getSuggestions())
                .actionVerbsFound(score.getActionVerbsFound())
                .actionVerbsMissing(score.getActionVerbsMissing())
                .quantifiableMetricsNotes(score.getQuantifiableMetricsNotes())
                .missingSections(score.getMissingSections())
                .dateConsistencyNotes(score.getDateConsistencyNotes())
                .evaluationDisclaimer(score.getEvaluationDisclaimer())
                .sectionBreakdownJson(score.getSectionBreakdownJson())
                .build();
    }
}
