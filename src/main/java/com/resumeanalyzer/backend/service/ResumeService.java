package com.resumeanalyzer.backend.service;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.dto.ResumeResponse;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.User;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import com.resumeanalyzer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeParserService resumeParserService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public ResumeResponse uploadResume(String userEmail, MultipartFile file) {

        if (file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException("Only PDF and DOCX files are allowed", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : "";
            String storedFileName = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            resumeRepository.findByUserAndActiveTrue(user)
                    .ifPresent(prev -> {
                        prev.setActive(false);
                        resumeRepository.save(prev);
                    });

            Resume resume = Resume.builder()
                    .user(user)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .filePath(filePath.toString())
                    .fileType(contentType.contains("pdf") ? "PDF" : "DOCX")
                    .fileSize(file.getSize())
                    .active(true)
                    .build();

            resumeRepository.save(resume);

            try {
                resumeParserService.parseAndSave(resume);
            } catch (Exception e) {
                System.err.println("Resume parsing failed: " + e.getMessage());
            }

            return mapToResponse(resume);

        } catch (IOException e) {
            throw new ApiException("Failed to store file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

        try {
            Files.deleteIfExists(Paths.get(resume.getFilePath()));
        } catch (IOException e) {
            throw new ApiException("Failed to delete file from storage", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        resumeRepository.delete(resume);
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .active(resume.isActive())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }

    public Resume getResumeEntityById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Resume not found", HttpStatus.NOT_FOUND));
    }
}