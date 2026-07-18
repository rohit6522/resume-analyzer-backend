package com.resumeanalyzer.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.entity.JobMatch;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.JobMatchRepository;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final GeminiService geminiService;
    private final JobMatchRepository jobMatchRepository;
    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobMatch matchResumeToJob(Resume resume, String jobDescription) {

        ParsedResumeData parsedData = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Resume must be parsed first", HttpStatus.BAD_REQUEST));

        String prompt = buildPrompt(parsedData.getRawText(), jobDescription);
        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);

        JobMatch jobMatch = parseResponse(cleanJson, resume, jobDescription);

        return jobMatchRepository.save(jobMatch);
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        return """
                You are an expert recruiter comparing a resume against a job description.
                Return ONLY a valid JSON object (no markdown, no explanation, no code fences)
                with exactly this structure:

                {
                  "matchPercentage": <0-100 integer, how well the resume matches the job>,
                  "matchedSkills": "<comma-separated list of skills/keywords found in both resume and job description>",
                  "missingSkills": "<comma-separated list of important skills from the job description missing in the resume>",
                  "recommendations": "<3-4 short actionable bullet points separated by newline, on how to tailor this resume for this job>"
                }

                Resume text:
                %s

                Job Description:
                %s
                """.formatted(resumeText, jobDescription);
    }

    private String cleanJsonResponse(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private JobMatch parseResponse(String json, Resume resume, String jobDescription) {
        try {
            JsonNode node = objectMapper.readTree(json);

            return JobMatch.builder()
                    .resume(resume)
                    .jobDescription(jobDescription)
                    .matchPercentage(node.path("matchPercentage").asInt())
                    .matchedSkills(node.path("matchedSkills").asText())
                    .missingSkills(node.path("missingSkills").asText())
                    .recommendations(node.path("recommendations").asText())
                    .build();

        } catch (Exception e) {
            throw new ApiException("Failed to parse AI response: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<JobMatch> getMatchesForResume(Long resumeId) {
        return jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resumeId);
    }
}