package com.resumeanalyzer.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.entity.AtsScore;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.AtsScoreRepository;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AtsService {

    private final GeminiService geminiService;
    private final AtsScoreRepository atsScoreRepository;
    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AtsScore generateAtsScore(Resume resume) {

        ParsedResumeData parsedData = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Resume must be parsed before scoring", HttpStatus.BAD_REQUEST));

        String prompt = buildPrompt(parsedData.getRawText());
        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);
        AtsScore atsScore = parseAtsResponse(cleanJson, resume);

        // Replace the old score only after the AI call succeeded (resume_id is unique)
        atsScoreRepository.findByResumeId(resume.getId()).ifPresent(existing -> {
            atsScoreRepository.delete(existing);
            atsScoreRepository.flush();
        });

        return atsScoreRepository.save(atsScore);
    }

    private String buildPrompt(String resumeText) {
        String currentDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        return """
            You are an expert ATS (Applicant Tracking System) resume evaluator.
            Today's date is %s. Use this as the reference point for all date-related checks.

            Analyze the following resume text and return ONLY a valid JSON object
            (no markdown, no explanation, no code fences) with exactly this structure:

            {
              "overallScore": <0-100 integer>,
              "formattingScore": <0-100 integer>,
              "keywordScore": <0-100 integer>,
              "grammarScore": <0-100 integer>,
              "projectsScore": <0-100 integer>,
              "experienceScore": <0-100 integer>,
              "educationScore": <0-100 integer>,
              "skillsScore": <0-100 integer>,
              "strengths": "<3-4 short bullet points separated by newline>",
              "weaknesses": "<3-4 short bullet points separated by newline>",
              "suggestions": "<3-4 actionable improvement suggestions separated by newline>",
              "dateConsistencyNotes": "<Check all dates in the resume against today's date. Flag any future-dated education/experience entries, incorrect total experience calculations, outdated-looking content (e.g. no activity in the last 2+ years), or inconsistent date ranges. If everything looks consistent and current, say so in one short line. Separate multiple notes by newline.>"
            }

            Resume text:
            %s
            """.formatted(currentDate, resumeText);
    }

    private String cleanJsonResponse(String rawResponse) {
        // Gemini sometimes wraps JSON in ```json ... ``` code fences — strip them
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private AtsScore parseAtsResponse(String json, Resume resume) {
        try {
            JsonNode node = objectMapper.readTree(json);

            return AtsScore.builder()
                    .resume(resume)
                    .overallScore(node.path("overallScore").asInt())
                    .formattingScore(node.path("formattingScore").asInt())
                    .keywordScore(node.path("keywordScore").asInt())
                    .grammarScore(node.path("grammarScore").asInt())
                    .projectsScore(node.path("projectsScore").asInt())
                    .experienceScore(node.path("experienceScore").asInt())
                    .educationScore(node.path("educationScore").asInt())
                    .skillsScore(node.path("skillsScore").asInt())
                    .strengths(node.path("strengths").asText())
                    .weaknesses(node.path("weaknesses").asText())
                    .suggestions(node.path("suggestions").asText())
                    .dateConsistencyNotes(node.path("dateConsistencyNotes").asText())
                    .build();

        } catch (Exception e) {
            throw new ApiException("Failed to parse AI response as JSON: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public AtsScore getAtsScoreByResumeId(Long resumeId) {
        return atsScoreRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ApiException("ATS score not generated yet for this resume", HttpStatus.NOT_FOUND));
    }
}