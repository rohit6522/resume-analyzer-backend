package com.resumeanalyzer.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPrepResponse {
    private String roleTitle;
    private Integer totalQuestions;
    private List<InterviewQuestion> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InterviewQuestion {
        private String id;
        private String category; // TECHNICAL, PROJECT_DEEP_DIVE, CODING_SYSTEM, BEHAVIORAL, HR, RESUME_VERIFICATION
        private String question;
        private String contextFromResume; // Which project/skill this question tests
        private String suggestedAnswer;
        private List<String> keyPointsToHit;
        private String commonPitfalls;
    }
}

