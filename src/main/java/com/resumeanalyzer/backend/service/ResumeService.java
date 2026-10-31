package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.ResumeResponse;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.User;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.AtsScoreRepository;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import com.resumeanalyzer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeParserService resumeParserService;
    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final AtsScoreRepository atsScoreRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public ResumeResponse uploadResume(String userEmail, MultipartFile file) {
        return uploadResume(userEmail, file, null, null);
    }

    public ResumeResponse uploadResume(String userEmail, MultipartFile file, String versionLabel, String targetRole) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is empty or not provided", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        boolean isPdf = (contentType != null && contentType.contains("pdf")) ||
                        (originalFileName != null && originalFileName.toLowerCase().endsWith(".pdf"));
        boolean isDocx = (contentType != null && contentType.contains("wordprocessingml")) ||
                         (originalFileName != null && originalFileName.toLowerCase().endsWith(".docx"));

        if (!isPdf && !isDocx) {
            throw new ApiException("Only PDF and DOCX files are allowed", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : (isPdf ? ".pdf" : ".docx");
            String storedFileName = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Deactivate previous active resumes
            resumeRepository.findByUserAndActiveTrue(user)
                    .ifPresent(prev -> {
                        prev.setActive(false);
                        resumeRepository.save(prev);
                    });

            String finalVersionLabel = (versionLabel != null && !versionLabel.isBlank())
                    ? versionLabel.trim()
                    : "Resume Version " + (resumeRepository.countByUser(user) + 1);

            Resume resume = Resume.builder()
                    .user(user)
                    .originalFileName(originalFileName != null ? originalFileName : "resume" + extension)
                    .storedFileName(storedFileName)
                    .filePath(filePath.toString())
                    .fileType(isPdf ? "PDF" : "DOCX")
                    .fileSize(file.getSize())
                    .versionLabel(finalVersionLabel)
                    .targetRole(targetRole != null && !targetRole.isBlank() ? targetRole.trim() : "General")
                    .active(true)
                    .build();

            resumeRepository.save(resume);

            try {
                resumeParserService.parseAndSave(resume);
            } catch (Exception e) {
                log.error("Resume parsing failed for resume ID {}: {}", resume.getId(), e.getMessage());
            }

            return mapToResponse(resume);

        } catch (IOException e) {
            throw new ApiException("Failed to store file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResumeResponse updateResumeMeta(String userEmail, Long resumeId, String versionLabel, String targetRole) {
        Resume resume = getOwnedResume(userEmail, resumeId);
        if (versionLabel != null && !versionLabel.isBlank()) {
            resume.setVersionLabel(versionLabel.trim());
        }
        if (targetRole != null && !targetRole.isBlank()) {
            resume.setTargetRole(targetRole.trim());
        }
        return mapToResponse(resumeRepository.save(resume));
    }

    public List<ResumeResponse> getUserResumes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return resumeRepository.findByUserOrderByUploadedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteResume(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ApiException("Resume not found", HttpStatus.NOT_FOUND));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ApiException("You are not authorized to delete this resume", HttpStatus.FORBIDDEN);
        }

        // Delete dependent records first (foreign key constraints)
        atsScoreRepository.findByResumeId(resumeId).ifPresent(atsScoreRepository::delete);
        parsedResumeDataRepository.findByResumeId(resumeId).ifPresent(parsedResumeDataRepository::delete);

        try {
            Files.deleteIfExists(Paths.get(resume.getFilePath()));
        } catch (IOException e) {
            log.warn("Failed to delete physical file from storage: {}", e.getMessage());
        }

        resumeRepository.delete(resume);
    }

    public ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .active(resume.isActive())
                .versionLabel(resume.getVersionLabel())
                .targetRole(resume.getTargetRole())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }

    public Resume getResumeEntityById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Resume not found", HttpStatus.NOT_FOUND));
    }

    public Resume getOwnedResume(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ApiException("Resume not found", HttpStatus.NOT_FOUND));

        // Return 404 (not 403) so we don't reveal that someone else's resume exists
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ApiException("Resume not found", HttpStatus.NOT_FOUND);
        }
        return resume;
    }
}
