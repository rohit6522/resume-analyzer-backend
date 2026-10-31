package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.SkillGapRequest;
import com.resumeanalyzer.backend.dto.SkillGapResponse;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerGuidanceServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private ParsedResumeDataRepository parsedResumeDataRepository;

    private CareerGuidanceService careerGuidanceService;

    @BeforeEach
    void setUp() {
        careerGuidanceService = new CareerGuidanceService(geminiService, parsedResumeDataRepository);
    }

    @Test
    void testAnalyzeSkillGapParsesCorrectly() {
        Long resumeId = 1L;
        Resume resume = Resume.builder().id(resumeId).build();
        ParsedResumeData data = ParsedResumeData.builder()
                .id(1L)
                .resume(resume)
                .rawText("Candidate experienced in React and TypeScript.")
                .build();

        when(parsedResumeDataRepository.findByResumeId(resumeId)).thenReturn(Optional.of(data));

        String mockAiResponse = """
                {
                  "readinessScore": 75,
                  "skillsOwned": ["React", "TypeScript"],
                  "skillsNeeded": ["Node.js", "Docker"],
                  "criticalMissing": ["Docker"],
                  "learningPriorities": [
                    {
                      "skillName": "Docker",
                      "priorityLevel": "CRITICAL",
                      "reason": "Containerization is required for deployments.",
                      "estimatedTimeToLearn": "1-2 weeks"
                    }
                  ],
                  "recommendedProjects": [
                    {
                      "title": "Full Stack Task Manager",
                      "description": "Containerized multi-service app.",
                      "techStack": "React, Node, Docker",
                      "portfolioImpact": "High"
                    }
                  ],
                  "roadmap": [
                    {
                      "stepNumber": 1,
                      "phaseTitle": "Containerization Basics",
                      "duration": "Week 1",
                      "keyMilestones": ["Write Dockerfiles", "Run Docker Compose"]
                    }
                  ]
                }
                """;

        when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

        SkillGapRequest req = SkillGapRequest.builder()
                .careerTrack("FULLSTACK")
                .targetCompanyOrLevel("Mid-Level")
                .build();

        SkillGapResponse response = careerGuidanceService.analyzeSkillGap(resume, req);

        assertNotNull(response);
        assertEquals("FULLSTACK", response.getCareerTrack());
        assertEquals(75, response.getReadinessScore());
        assertEquals(2, response.getSkillsOwned().size());
        assertEquals(2, response.getSkillsNeeded().size());
        assertEquals(1, response.getCriticalMissing().size());
        assertEquals("Docker", response.getCriticalMissing().get(0));
    }
}

