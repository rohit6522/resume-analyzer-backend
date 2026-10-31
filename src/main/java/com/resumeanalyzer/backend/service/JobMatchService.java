package com.resumeanalyzer.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.resumeanalyzer.backend.dto.JobMatchResponse;
import com.resumeanalyzer.backend.entity.JobMatch;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.JobMatchRepository;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final GeminiService geminiService;
    private final JobMatchRepository jobMatchRepository;
    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobMatch matchResumeToJob(Resume resume, String jobDescription) {
        return matchResumeToJob(resume, jobDescription, null, null);
    }

    public JobMatch matchResumeToJob(Resume resume, String jobDescription, String jobTitle, String companyName) {
        if (jobDescription == null || jobDescription.trim().length() < 20) {
            throw new ApiException("Job description is too short. Please provide a complete job description.", HttpStatus.BAD_REQUEST);
        }

        ParsedResumeData parsedData = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Resume must be parsed first", HttpStatus.BAD_REQUEST));

        String prompt = buildPrompt(parsedData.getRawText(), jobDescription, jobTitle, companyName);
        String aiResponse = geminiService.generateContent(prompt);
        String cleanJson = cleanJsonResponse(aiResponse);

        JobMatch jobMatch = parseResponse(cleanJson, resume, jobDescription, jobTitle, companyName);

        return jobMatchRepository.save(jobMatch);
    }

    public JobMatch matchResumeToJobFile(Resume resume, MultipartFile file, String jobTitle, String companyName) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Please upload a valid job description document.", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extractedText;

        try (InputStream is = file.getInputStream()) {
            if (filename.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    extractedText = stripper.getText(doc);
                }
            } else if (filename.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(is)) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph p : doc.getParagraphs()) {
                        sb.append(p.getText()).append("\n");
                    }
                    extractedText = sb.toString();
                }
            } else {
                throw new ApiException("Only PDF and DOCX files are supported for job description upload.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("Failed to parse uploaded job description file", e);
            throw new ApiException("Failed to parse job description file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return matchResumeToJob(resume, extractedText, jobTitle, companyName);
    }

    private String buildPrompt(String resumeText, String jobDescription, String jobTitle, String companyName) {
        String roleHeader = (jobTitle != null && !jobTitle.isBlank()) ? "Target Job Title: " + jobTitle + "\n" : "";
        String companyHeader = (companyName != null && !companyName.isBlank()) ? "Target Company: " + companyName + "\n" : "";

        return """
                You are a senior technical recruiter and ATS matcher.
                Compare the candidate's resume strictly against the target job description.
                %s%s
                CRITICAL RULE: NEVER mark a skill or qualification as matched or present unless there is clear, explicit evidence in the candidate's resume text. If it is not mentioned in the resume, it MUST be classified as missing.
                
                Return ONLY a valid JSON object (no markdown, no code fences):
                {
                  "matchPercentage": <integer 0-100 indicating honest job readiness match>,
                  "matchedTechnicalSkills": "<comma-separated list of technical skills found in BOTH the resume and the job description>",
                  "missingTechnicalSkills": "<comma-separated list of required or preferred technical skills from the job description missing in the resume>",
                  "matchedKeywords": "<comma-separated list of important domain keywords, tools, or methodologies present in both>",
                  "missingKeywords": "<comma-separated list of critical keywords present in the job description but absent from the resume>",
                  "relevantExperience": "<2-3 sentence summary of candidate experience that directly supports this role>",
                  "experienceGaps": "<2-3 sentence summary of gaps in years of experience, seniority, domain background, or responsibilities>",
                  "educationRequirements": "<Summary of how candidate education matches the job requirement, e.g. 'Meets degree requirement' or 'Job asks for Master's degree; candidate holds Bachelor's'>",
                  "recommendedSkillsToLearn": "<comma-separated list of top 3-5 high-ROI skills or certifications candidate should acquire>",
                  "recommendations": "<3-5 prioritized, concrete bullet points separated by newline explaining exactly how to tailor this resume for this job>"
                }

                Candidate Resume Text:
                %s

                Job Description:
                %s
                """.formatted(
                roleHeader,
                companyHeader,
                resumeText.length() > 6500 ? resumeText.substring(0, 6500) : resumeText,
                jobDescription.length() > 5000 ? jobDescription.substring(0, 5000) : jobDescription
        );
    }

    private String cleanJsonResponse(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }
        return cleaned;
    }

    private JobMatch parseResponse(String json, Resume resume, String jobDescription, String jobTitle, String companyName) {
        try {
            JsonNode node = objectMapper.readTree(json);

            String matchedTech = node.path("matchedTechnicalSkills").asText("");
            String missingTech = node.path("missingTechnicalSkills").asText("");
            String matchedKw = node.path("matchedKeywords").asText("");
            String missingKw = node.path("missingKeywords").asText("");

            String combinedMatched = !matchedTech.isBlank() && !matchedKw.isBlank()
                    ? matchedTech + ", " + matchedKw
                    : (!matchedTech.isBlank() ? matchedTech : matchedKw);

            String combinedMissing = !missingTech.isBlank() && !missingKw.isBlank()
                    ? missingTech + ", " + missingKw
                    : (!missingTech.isBlank() ? missingTech : missingKw);

            return JobMatch.builder()
                    .resume(resume)
                    .jobTitle(jobTitle)
                    .companyName(companyName)
                    .jobDescription(jobDescription)
                    .matchPercentage(node.path("matchPercentage").asInt(50))
                    .matchedSkills(combinedMatched)
                    .missingSkills(combinedMissing)
                    .matchedTechnicalSkills(matchedTech)
                    .missingTechnicalSkills(missingTech)
                    .matchedKeywords(matchedKw)
                    .missingKeywords(missingKw)
                    .relevantExperience(node.path("relevantExperience").asText("Relevant technical background."))
                    .experienceGaps(node.path("experienceGaps").asText("Review specific domain tooling experience."))
                    .educationRequirements(node.path("educationRequirements").asText("Meets baseline educational qualifications."))
                    .recommendedSkillsToLearn(node.path("recommendedSkillsToLearn").asText(missingTech))
                    .recommendations(node.path("recommendations").asText("Tailor project descriptions with keywords from job posting."))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse JobMatch response JSON: {}", json, e);
            throw new ApiException("Failed to parse job match response: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<JobMatch> getMatchesForResume(Long resumeId) {
        return jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resumeId);
    }

    public JobMatchResponse mapJobMatchToResponse(JobMatch match) {
        return JobMatchResponse.builder()
                .id(match.getId())
                .resumeId(match.getResume().getId())
                .jobTitle(match.getJobTitle())
                .companyName(match.getCompanyName())
                .matchPercentage(match.getMatchPercentage())
                .matchedSkills(match.getMatchedSkills())
                .missingSkills(match.getMissingSkills())
                .matchedTechnicalSkills(match.getMatchedTechnicalSkills())
                .missingTechnicalSkills(match.getMissingTechnicalSkills())
                .matchedKeywords(match.getMatchedKeywords())
                .missingKeywords(match.getMissingKeywords())
                .relevantExperience(match.getRelevantExperience())
                .experienceGaps(match.getExperienceGaps())
                .educationRequirements(match.getEducationRequirements())
                .recommendedSkillsToLearn(match.getRecommendedSkillsToLearn())
                .recommendations(match.getRecommendations())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
