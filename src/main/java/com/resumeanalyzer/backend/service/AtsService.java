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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
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

        String prompt = buildPrompt(parsedData.getRawText(), parsedData);
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

    private String buildPrompt(String resumeText, ParsedResumeData parsedData) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        return """
            You are an advanced, industry-standard ATS (Applicant Tracking System) resume analyzer.
            Today's reference date is %s.
            
            Evaluate the following candidate resume thoroughly across 7 transparent evaluation criteria.
            Do not pretend to be a proprietary ATS algorithm; apply rigorous, transparent recruitment heuristics.
            
            Return ONLY a valid JSON object (no markdown, no code fences, no explanatory preamble):
            {
              "overallScore": <integer 0-100 calculated from weighted criteria below>,
              "skillsScore": <integer 0-100, depth of technical and core skills, keyword breadth>,
              "experienceScore": <integer 0-100, progression, responsibility depth, relevance>,
              "projectsScore": <integer 0-100, complexity, technical stack demonstration, impact>,
              "educationScore": <integer 0-100, degree relevance, credentials, certifications>,
              "formattingScore": <integer 0-100, layout cleanliness, standard headings, parseability>,
              "contentQualityScore": <integer 0-100, tone, conciseness, grammar, spelling>,
              "atsCompatibilityScore": <integer 0-100, machine readability, single-column vs tables, standard headers>,
              "keywordScore": <integer 0-100, industry keyword density>,
              "grammarScore": <integer 0-100, grammar accuracy and clarity>,
              "actionVerbsScore": <integer 0-100, frequency of strong action verbs at start of bullets>,
              "quantifiableMetricsScore": <integer 0-100, presence of measurable outcomes, percentages, figures>,
              "strengths": "<3-5 concrete bullet points separated by newline detailing what the resume does exceptionally well>",
              "weaknesses": "<3-5 concrete bullet points separated by newline detailing areas where the resume is lacking>",
              "suggestions": "<4-5 actionable, high-priority improvement recommendations separated by newline>",
              "actionVerbsFound": "<comma-separated list of strong action verbs detected, e.g. Engineered, Spearheaded, Optimized>",
              "actionVerbsMissing": "<comma-separated list of recommended power verbs candidate should use instead of passive phrases>",
              "quantifiableMetricsNotes": "<Brief assessment of whether bullet points include measurable achievements (e.g. 'Improved speed by 30%%') and suggestions for where to add metrics>",
              "missingSections": "<Comma-separated list of missing or weak standard sections (e.g., Missing Certifications, Weak Summary) or 'None' if complete>",
              "dateConsistencyNotes": "<Chronological check against reference date. Flags any gaps, future dates, overlapping periods, or 'All dates are consistent and current.'>",
              "evaluationDisclaimer": "This ATS score is an estimated evaluation based on transparent industry heuristics and standard recruitment criteria, not proprietary algorithms."
            }

            Candidate Resume Text:
            %s
            """.formatted(currentDate, resumeText.length() > 7000 ? resumeText.substring(0, 7000) : resumeText);
    }

    private String cleanJsonResponse(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private AtsScore parseAtsResponse(String json, Resume resume) {
        try {
            JsonNode node = objectMapper.readTree(json);

            int overall = node.path("overallScore").asInt(70);
            int skills = node.path("skillsScore").asInt(70);
            int experience = node.path("experienceScore").asInt(70);
            int projects = node.path("projectsScore").asInt(70);
            int education = node.path("educationScore").asInt(70);
            int formatting = node.path("formattingScore").asInt(70);
            int content = node.path("contentQualityScore").asInt(70);
            int atsCompat = node.path("atsCompatibilityScore").asInt(70);

            // If overall score wasn't properly weighted, calculate transparent weighted score:
            // Skills: 20%, Exp: 20%, Projects: 15%, Edu: 10%, Formatting: 15%, Content: 10%, ATS: 10%
            if (overall <= 0 || overall > 100) {
                overall = (int) Math.round(
                        (skills * 0.20) + (experience * 0.20) + (projects * 0.15) +
                        (education * 0.10) + (formatting * 0.15) + (content * 0.10) + (atsCompat * 0.10)
                );
            }

            return AtsScore.builder()
                    .resume(resume)
                    .overallScore(overall)
                    .skillsScore(skills)
                    .experienceScore(experience)
                    .projectsScore(projects)
                    .educationScore(education)
                    .formattingScore(formatting)
                    .contentQualityScore(content)
                    .atsCompatibilityScore(atsCompat)
                    .keywordScore(node.path("keywordScore").asInt(skills))
                    .grammarScore(node.path("grammarScore").asInt(content))
                    .actionVerbsScore(node.path("actionVerbsScore").asInt(65))
                    .quantifiableMetricsScore(node.path("quantifiableMetricsScore").asInt(60))
                    .strengths(node.path("strengths").asText("Strong technical foundations.\nClear academic background."))
                    .weaknesses(node.path("weaknesses").asText("Lacks quantifiable metrics in achievements.\nCould expand on project architectures."))
                    .suggestions(node.path("suggestions").asText("Incorporate measurable numbers into project bullets.\nHighlight specific framework versions used."))
                    .actionVerbsFound(node.path("actionVerbsFound").asText("Developed, Built, Created"))
                    .actionVerbsMissing(node.path("actionVerbsMissing").asText("Spearheaded, Architected, Accelerated, Reduced"))
                    .quantifiableMetricsNotes(node.path("quantifiableMetricsNotes").asText("Add metrics: mention team sizes, % performance improvements, user counts."))
                    .missingSections(node.path("missingSections").asText("None"))
                    .dateConsistencyNotes(node.path("dateConsistencyNotes").asText("All timeline dates appear consistent."))
                    .evaluationDisclaimer(node.path("evaluationDisclaimer").asText(
                            "This ATS score is an estimated evaluation based on transparent industry heuristics and standard recruitment criteria, not proprietary algorithms."))
                    .sectionBreakdownJson(json)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse ATS response JSON: {}", json, e);
            throw new ApiException("Failed to parse AI evaluation response: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public AtsScore getAtsScoreByResumeId(Long resumeId) {
        return atsScoreRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ApiException("ATS score not generated yet for this resume", HttpStatus.NOT_FOUND));
    }
}
