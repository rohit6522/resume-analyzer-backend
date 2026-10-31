package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerDashboardResponse {
    private long totalResumes;
    private Integer latestAtsScore;
    private Double averageAtsScore;
    private Integer bestAtsScore;
    private String bestResumeName;
    private Long bestResumeId;

    private long totalJobMatches;
    private Double averageJobMatch;

    private long totalApplications;
    private long activeApplications;
    private long offersCount;
    private Map<String, Long> applicationsByStatus;

    private List<ResumeResponse> recentResumes;
    private List<ScoreHistoryItem> scoreHistory;
    private List<String> topMissingSkills;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreHistoryItem {
        private String resumeName;
        private Long resumeId;
        private Integer overallScore;
        private Integer formattingScore;
        private Integer skillsScore;
        private Integer experienceScore;
        private String uploadedDate;
    }
}

