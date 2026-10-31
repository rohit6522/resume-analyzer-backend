package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.JobApplicationDto;
import com.resumeanalyzer.backend.entity.JobApplication;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.User;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.JobApplicationRepository;
import com.resumeanalyzer.backend.repository.ResumeRepository;
import com.resumeanalyzer.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    public List<JobApplicationDto> getUserApplications(String email) {
        User user = getUser(email);
        return applicationRepository.findByUserOrderByAppliedDateDesc(user)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public JobApplicationDto createApplication(String email, JobApplicationDto dto) {
        User user = getUser(email);

        Resume resume = null;
        if (dto.getResumeId() != null) {
            resume = resumeRepository.findById(dto.getResumeId()).orElse(null);
        }

        JobApplication app = JobApplication.builder()
                .user(user)
                .resume(resume)
                .companyName(dto.getCompanyName())
                .jobTitle(dto.getJobTitle())
                .status(dto.getStatus() != null ? dto.getStatus() : "APPLIED")
                .salaryRange(dto.getSalaryRange())
                .jobUrl(dto.getJobUrl())
                .matchScore(dto.getMatchScore())
                .notes(dto.getNotes())
                .appliedDate(dto.getAppliedDate() != null ? dto.getAppliedDate() : LocalDate.now())
                .build();

        return mapToDto(applicationRepository.save(app));
    }

    public JobApplicationDto updateApplication(String email, Long id, JobApplicationDto dto) {
        User user = getUser(email);
        JobApplication app = applicationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException("Application not found", HttpStatus.NOT_FOUND));

        if (dto.getCompanyName() != null) app.setCompanyName(dto.getCompanyName());
        if (dto.getJobTitle() != null) app.setJobTitle(dto.getJobTitle());
        if (dto.getStatus() != null) app.setStatus(dto.getStatus());
        if (dto.getSalaryRange() != null) app.setSalaryRange(dto.getSalaryRange());
        if (dto.getJobUrl() != null) app.setJobUrl(dto.getJobUrl());
        if (dto.getMatchScore() != null) app.setMatchScore(dto.getMatchScore());
        if (dto.getNotes() != null) app.setNotes(dto.getNotes());
        if (dto.getAppliedDate() != null) app.setAppliedDate(dto.getAppliedDate());

        if (dto.getResumeId() != null) {
            Resume resume = resumeRepository.findById(dto.getResumeId()).orElse(null);
            app.setResume(resume);
        }

        return mapToDto(applicationRepository.save(app));
    }

    public void deleteApplication(String email, Long id) {
        User user = getUser(email);
        JobApplication app = applicationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException("Application not found", HttpStatus.NOT_FOUND));
        applicationRepository.delete(app);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private JobApplicationDto mapToDto(JobApplication a) {
        return JobApplicationDto.builder()
                .id(a.getId())
                .resumeId(a.getResume() != null ? a.getResume().getId() : null)
                .companyName(a.getCompanyName())
                .jobTitle(a.getJobTitle())
                .status(a.getStatus())
                .salaryRange(a.getSalaryRange())
                .jobUrl(a.getJobUrl())
                .matchScore(a.getMatchScore())
                .notes(a.getNotes())
                .appliedDate(a.getAppliedDate())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

