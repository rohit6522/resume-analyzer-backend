package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapResponse {
    private String careerTrack;
    private Integer readinessScore; // 0-100%
    private List<String> skillsOwned;
    private List<String> skillsNeeded;
    private List<String> criticalMissing;
    private List<LearningPriorityItem> learningPriorities;
    private List<ProjectRecommendation> recommendedProjects;
    private List<RoadmapStep> roadmap;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningPriorityItem {
        private String skillName;
        private String priorityLevel; // CRITICAL, HIGH, MEDIUM
        private String reason;
        private String estimatedTimeToLearn; // e.g. "2-3 weeks"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectRecommendation {
        private String title;
        private String description;
        private String techStack;
        private String portfolioImpact;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadmapStep {
        private Integer stepNumber;
        private String phaseTitle;
        private String duration;
        private List<String> keyMilestones;
    }
}

