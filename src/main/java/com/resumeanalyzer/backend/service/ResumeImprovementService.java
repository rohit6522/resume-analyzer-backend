package com.resumeanalyzer.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.dto.ImprovementRequest;
import com.resumeanalyzer.backend.dto.ImprovementResponse;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeImprovementService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImprovementResponse improve(Resume resume, ImprovementRequest request) {
        String type = request.getImprovementType() != null ? request.getImprovementType().toUpperCase() : "GENERAL";
        String prompt = buildImprovementPrompt(request.getText(), type, request.getTargetRole(), request.getContext());

        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);

        return parseImprovementResponse(cleanJson, request.getText(), type);
    }

    private String buildImprovementPrompt(String text, String type, String targetRole, String context) {
        String targetRoleInstruction = (targetRole != null && !targetRole.isBlank())
                ? "Target Career/Job Role: " + targetRole + "\n" : "";
        String contextInstruction = (context != null && !context.isBlank())
                ? "Additional Context: " + context + "\n" : "";

        return """
                You are an elite executive resume writer and career coach.
                Your task is to improve the provided resume text based on the mode: %s.
                %s%s
                
                STRICT ANTI-FABRICATION RULE:
                - NEVER invent or hallucinate companies, degrees, certifications, numerical results, technologies, or achievements that the candidate did not mention.
                - If quantifiable data (e.g., percentages, scale, revenue, performance gains) would make a bullet point stronger, use clear placeholders like `[e.g. by X%%]` or `[e.g. supporting N users]` and list specific questions in `questionsForUser` asking the candidate for their real numbers.
                - When improving bullet points, apply the Google XYZ formula: "Accomplished [X], as measured by [Y], by doing [Z]".
                - Lead with decisive action verbs (e.g. Engineered, Spearheaded, Optimized, Orchestrated, Streamlined).

                Return ONLY a valid JSON object (no markdown, no code fences):
                {
                  "improvedText": "<The improved, polished, professional version of the text>",
                  "changesSummary": "<A 1-2 sentence concise summary of what was enhanced>",
                  "questionsForUser": ["<Question 1 asking for real missing metrics/details>", "<Question 2>"],
                  "powerVerbsUsed": ["<Verb 1>", "<Verb 2>", "<Verb 3>"],
                  "rationale": "<Why these specific changes increase ATS score and recruiter engagement>"
                }

                Original Text:
                %s
                """.formatted(
                type,
                targetRoleInstruction,
                contextInstruction,
                text.length() > 4000 ? text.substring(0, 4000) : text
        );
    }

    private String cleanJsonResponse(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private ImprovementResponse parseImprovementResponse(String json, String originalText, String type) {
        try {
            JsonNode node = objectMapper.readTree(json);

            List<String> questions = new ArrayList<>();
            JsonNode qNode = node.path("questionsForUser");
            if (qNode.isArray()) {
                qNode.forEach(item -> questions.add(item.asText()));
            }

            List<String> verbs = new ArrayList<>();
            JsonNode vNode = node.path("powerVerbsUsed");
            if (vNode.isArray()) {
                vNode.forEach(item -> verbs.add(item.asText()));
            }

            return ImprovementResponse.builder()
                    .originalText(originalText)
                    .improvedText(node.path("improvedText").asText(originalText))
                    .improvementType(type)
                    .changesSummary(node.path("changesSummary").asText("Enhanced clarity, impact, and action verb strength."))
                    .questionsForUser(questions)
                    .powerVerbsUsed(verbs)
                    .rationale(node.path("rationale").asText("Stronger action orientation increases ATS ranking and recruiter interest."))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Improvement response: {}", json, e);
            throw new ApiException("Failed to parse improvement suggestions: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

