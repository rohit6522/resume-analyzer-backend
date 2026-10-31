package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.CareerDashboardResponse;
import com.resumeanalyzer.backend.dto.ResumeResponse;
import com.resumeanalyzer.backend.entity.AtsScore;
import com.resumeanalyzer.backend.entity.JobMatch;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.User;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final ResumeRepository resumeRepository;
    private final AtsScoreRepository atsScoreRepository;
    private final JobMatchRepository jobMatchRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ResumeService resumeService;

    public CareerDashboardResponse getDashboardAnalytics(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<Resume> userResumes = resumeRepository.findByUserOrderByUploadedAtDesc(user);
        long totalResumes = userResumes.size();

        Integer latestScore = null;
        Integer bestScore = null;
        String bestResumeName = null;
        Long bestResumeId = null;
        double sumScore = 0;
        int scoredCount = 0;

        List<CareerDashboardResponse.ScoreHistoryItem> scoreHistory = new ArrayList<>();

        for (Resume resume : userResumes) {
            Optional<AtsScore> scoreOpt = atsScoreRepository.findByResumeId(resume.getId());
            if (scoreOpt.isPresent()) {
                AtsScore score = scoreOpt.get();
                int val = score.getOverallScore() != null ? score.getOverallScore() : 0;
                sumScore += val;
                scoredCount++;

                if (latestScore == null) {
                    latestScore = val;
                }

                if (bestScore == null || val > bestScore) {
                    bestScore = val;
                    bestResumeName = resume.getOriginalFileName();
                    bestResumeId = resume.getId();
                }

                scoreHistory.add(CareerDashboardResponse.ScoreHistoryItem.builder()
                        .resumeId(resume.getId())
                        .resumeName(resume.getOriginalFileName())
                        .overallScore(val)
                        .formattingScore(score.getFormattingScore())
                        .skillsScore(score.getSkillsScore())
                        .experienceScore(score.getExperienceScore())
                        .uploadedDate(resume.getUploadedAt() != null
                                ? resume.getUploadedAt().format(DateTimeFormatter.ofPattern("MMM d"))
                                : "")
                        .build());
            }
        }

        Double avgAtsScore = scoredCount > 0 ? Math.round((sumScore / scoredCount) * 10.0) / 10.0 : null;

        // Job matches analytics
        long totalJobMatches = 0;
        double matchSum = 0;
        Map<String, Integer> missingSkillFrequency = new HashMap<>();

        for (Resume resume : userResumes) {
            List<JobMatch> matches = jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resume.getId());
            totalJobMatches += matches.size();
            for (JobMatch jm : matches) {
                if (jm.getMatchPercentage() != null) {
                    matchSum += jm.getMatchPercentage();
                }
                if (jm.getMissingSkills() != null) {
                    for (String s : jm.getMissingSkills().split(",")) {
                        String trimmed = s.trim();
                        if (!trimmed.isBlank()) {
                            missingSkillFrequency.put(trimmed, missingSkillFrequency.getOrDefault(trimmed, 0) + 1);
                        }
                    }
                }
            }
        }

        Double avgJobMatch = totalJobMatches > 0 ? Math.round((matchSum / totalJobMatches) * 10.0) / 10.0 : null;

        List<String> topMissingSkills = missingSkillFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Applications analytics
        long totalApps = applicationRepository.countByUser(user);
        long appliedCount = applicationRepository.countByUserAndStatus(user, "APPLIED");
        long interviewCount = applicationRepository.countByUserAndStatus(user, "INTERVIEWING");
        long offerCount = applicationRepository.countByUserAndStatus(user, "OFFER");
        long wishlistCount = applicationRepository.countByUserAndStatus(user, "WISHLIST");
        long rejectedCount = applicationRepository.countByUserAndStatus(user, "REJECTED");

        Map<String, Long> statusMap = Map.of(
                "WISHLIST", wishlistCount,
                "APPLIED", appliedCount,
                "INTERVIEWING", interviewCount,
                "OFFER", offerCount,
                "REJECTED", rejectedCount
        );

        List<ResumeResponse> recentResumes = userResumes.stream()
                .limit(5)
                .map(resumeService::mapToResponse)
                .collect(Collectors.toList());

        return CareerDashboardResponse.builder()
                .totalResumes(totalResumes)
                .latestAtsScore(latestScore)
                .averageAtsScore(avgAtsScore)
                .bestAtsScore(bestScore)
                .bestResumeName(bestResumeName)
                .bestResumeId(bestResumeId)
                .totalJobMatches(totalJobMatches)
                .averageJobMatch(avgJobMatch)
                .totalApplications(totalApps)
                .activeApplications(appliedCount + interviewCount)
                .offersCount(offerCount)
                .applicationsByStatus(statusMap)
                .recentResumes(recentResumes)
                .scoreHistory(scoreHistory)
                .topMissingSkills(topMissingSkills)
                .build();
    }
}

