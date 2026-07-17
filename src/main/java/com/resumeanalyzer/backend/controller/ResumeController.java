package com.resumeanalyzer.backend.controller;
import com.resumeanalyzer.backend.dto.ParsedResumeResponse;
import com.resumeanalyzer.backend.dto.ResumeResponse;
import com.resumeanalyzer.backend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.resumeanalyzer.backend.service.ResumeParserService;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeParserService resumeParserService;
    @PostMapping("/upload")
    public ResponseEntity<ResumeResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String email = authentication.getName();
        ResumeResponse response = resumeService.uploadResume(email, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getMyResumes(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(resumeService.getUserResumes(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        resumeService.deleteResume(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/parsed-data")
    public ResponseEntity<ParsedResumeResponse> getParsedData(@PathVariable Long id) {
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
}