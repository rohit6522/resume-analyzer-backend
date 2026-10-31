package com.resumeanalyzer.backend.service;

import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.dto.ResumeBuilderDto;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.ResumeBuilderProfile;
import com.resumeanalyzer.backend.entity.User;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import com.resumeanalyzer.backend.repository.ResumeBuilderProfileRepository;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import com.resumeanalyzer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeBuilderService {

    private final ResumeBuilderProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ParsedResumeDataRepository parsedDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ResumeBuilderDto> getUserProfiles(String email) {
        User user = getUser(email);
        return profileRepository.findByUserOrderByUpdatedAtDesc(user)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ResumeBuilderDto getProfileById(String email, Long id) {
        User user = getUser(email);
        ResumeBuilderProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException("Resume profile not found", HttpStatus.NOT_FOUND));
        return mapToDto(profile);
    }

    public ResumeBuilderDto createProfile(String email, ResumeBuilderDto dto) {
        User user = getUser(email);

        Resume resume = null;
        if (dto.getResumeId() != null) {
            resume = resumeRepository.findById(dto.getResumeId()).orElse(null);
        }

        ResumeBuilderProfile profile = ResumeBuilderProfile.builder()
                .user(user)
                .resume(resume)
                .title(dto.getTitle())
                .templateName(dto.getTemplateName() != null ? dto.getTemplateName() : "MODERN_PROFESSIONAL")
                .profileDataJson(dto.getProfileDataJson())
                .build();

        return mapToDto(profileRepository.save(profile));
    }

    public ResumeBuilderDto updateProfile(String email, Long id, ResumeBuilderDto dto) {
        User user = getUser(email);
        ResumeBuilderProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException("Resume profile not found", HttpStatus.NOT_FOUND));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            profile.setTitle(dto.getTitle());
        }
        if (dto.getTemplateName() != null && !dto.getTemplateName().isBlank()) {
            profile.setTemplateName(dto.getTemplateName());
        }
        if (dto.getProfileDataJson() != null) {
            profile.setProfileDataJson(dto.getProfileDataJson());
        }

        return mapToDto(profileRepository.save(profile));
    }

    public void deleteProfile(String email, Long id) {
        User user = getUser(email);
        ResumeBuilderProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException("Resume profile not found", HttpStatus.NOT_FOUND));
        profileRepository.delete(profile);
    }

    public ResumeBuilderDto importFromResume(String email, Long resumeId) {
        User user = getUser(email);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ApiException("Resume not found", HttpStatus.NOT_FOUND));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ApiException("Not authorized", HttpStatus.FORBIDDEN);
        }

        ParsedResumeData data = parsedDataRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ApiException("Parsed data not found for this resume", HttpStatus.NOT_FOUND));

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("personalInfo", Map.of(
                "fullName", data.getFullName() != null ? data.getFullName() : "",
                "email", data.getEmail() != null ? data.getEmail() : "",
                "phone", data.getPhone() != null ? data.getPhone() : "",
                "linkedin", data.getLinkedin() != null ? data.getLinkedin() : "",
                "github", data.getGithub() != null ? data.getGithub() : "",
                "portfolio", data.getPortfolio() != null ? data.getPortfolio() : "",
                "location", ""
        ));
        initialData.put("summary", data.getSummary() != null ? data.getSummary() : "");
        initialData.put("technicalSkills", data.getTechnicalSkills() != null ? data.getTechnicalSkills() : data.getSkills());
        initialData.put("softSkills", data.getSoftSkills() != null ? data.getSoftSkills() : "");
        initialData.put("experienceText", data.getWorkExperience() != null ? data.getWorkExperience() : "");
        initialData.put("educationText", data.getEducation() != null ? data.getEducation() : "");
        initialData.put("projectsText", data.getProjects() != null ? data.getProjects() : "");
        initialData.put("certificationsText", data.getCertifications() != null ? data.getCertifications() : "");
        initialData.put("achievementsText", data.getAchievements() != null ? data.getAchievements() : "");

        String json;
        try {
            json = objectMapper.writeValueAsString(initialData);
        } catch (Exception e) {
            json = "{}";
        }

        String title = (data.getFullName() != null ? data.getFullName() + "'s Resume" : "Imported Resume") +
                       " (" + resume.getFileType() + ")";

        ResumeBuilderProfile profile = ResumeBuilderProfile.builder()
                .user(user)
                .resume(resume)
                .title(title)
                .templateName("MODERN_PROFESSIONAL")
                .profileDataJson(json)
                .build();

        return mapToDto(profileRepository.save(profile));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private ResumeBuilderDto mapToDto(ResumeBuilderProfile p) {
        return ResumeBuilderDto.builder()
                .id(p.getId())
                .resumeId(p.getResume() != null ? p.getResume().getId() : null)
                .title(p.getTitle())
                .templateName(p.getTemplateName())
                .profileDataJson(p.getProfileDataJson())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

