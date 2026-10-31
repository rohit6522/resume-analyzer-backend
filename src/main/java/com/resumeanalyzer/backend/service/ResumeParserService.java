package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.UpdateParsedResumeRequest;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParserService {

    private final ParsedResumeDataRepository parsedResumeDataRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,5}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,5}");

    private static final Pattern LINKEDIN_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_/]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[a-zA-Z0-9\\-_]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern PORTFOLIO_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?[a-zA-Z0-9.-]+\\.(?:dev|me|io|app|tech|info|co)(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);

    public ParsedResumeData parseAndSave(Resume resume) {
        String extractedText = extractText(resume.getFilePath(), resume.getFileType());

        if (extractedText == null || extractedText.trim().length() < 20) {
            throw new ApiException("Extracted resume text is too short or empty. Please ensure the document contains selectable text.", HttpStatus.BAD_REQUEST);
        }

        // Heuristic section separation
        Map<String, String> sections = segmentSections(extractedText);

        String email = extractFirst(EMAIL_PATTERN, extractedText);
        String phone = extractFirst(PHONE_PATTERN, extractedText);
        String linkedin = extractFirst(LINKEDIN_PATTERN, extractedText);
        String github = extractFirst(GITHUB_PATTERN, extractedText);
        String portfolio = extractPortfolio(extractedText, linkedin, github);
        String fullName = guessFullName(extractedText);

        String summary = sections.getOrDefault("summary", "");
        String rawSkills = sections.getOrDefault("skills", "");
        String experience = sections.getOrDefault("experience", "");
        String education = sections.getOrDefault("education", "");
        String projects = sections.getOrDefault("projects", "");
        String certifications = sections.getOrDefault("certifications", "");
        String achievements = sections.getOrDefault("achievements", "");

        // Try to parse categorized technical/soft skills
        String technicalSkills = rawSkills;
        String softSkills = "";

        // Attempt AI enhancement if available to structure details accurately
        try {
            ParsedResumeData aiEnhanced = enhanceWithAi(extractedText);
            if (aiEnhanced != null) {
                if (aiEnhanced.getFullName() != null && !aiEnhanced.getFullName().isBlank()) fullName = aiEnhanced.getFullName();
                if (aiEnhanced.getEmail() != null && !aiEnhanced.getEmail().isBlank()) email = aiEnhanced.getEmail();
                if (aiEnhanced.getPhone() != null && !aiEnhanced.getPhone().isBlank()) phone = aiEnhanced.getPhone();
                if (aiEnhanced.getLinkedin() != null && !aiEnhanced.getLinkedin().isBlank()) linkedin = aiEnhanced.getLinkedin();
                if (aiEnhanced.getGithub() != null && !aiEnhanced.getGithub().isBlank()) github = aiEnhanced.getGithub();
                if (aiEnhanced.getPortfolio() != null && !aiEnhanced.getPortfolio().isBlank()) portfolio = aiEnhanced.getPortfolio();
                if (aiEnhanced.getSummary() != null && !aiEnhanced.getSummary().isBlank()) summary = aiEnhanced.getSummary();
                if (aiEnhanced.getTechnicalSkills() != null && !aiEnhanced.getTechnicalSkills().isBlank()) technicalSkills = aiEnhanced.getTechnicalSkills();
                if (aiEnhanced.getSoftSkills() != null && !aiEnhanced.getSoftSkills().isBlank()) softSkills = aiEnhanced.getSoftSkills();
                if (aiEnhanced.getWorkExperience() != null && !aiEnhanced.getWorkExperience().isBlank()) experience = aiEnhanced.getWorkExperience();
                if (aiEnhanced.getEducation() != null && !aiEnhanced.getEducation().isBlank()) education = aiEnhanced.getEducation();
                if (aiEnhanced.getProjects() != null && !aiEnhanced.getProjects().isBlank()) projects = aiEnhanced.getProjects();
                if (aiEnhanced.getCertifications() != null && !aiEnhanced.getCertifications().isBlank()) certifications = aiEnhanced.getCertifications();
                if (aiEnhanced.getAchievements() != null && !aiEnhanced.getAchievements().isBlank()) achievements = aiEnhanced.getAchievements();
            }
        } catch (Exception e) {
            log.warn("AI-assisted parsing enhancement skipped or failed, falling back to heuristic parser: {}", e.getMessage());
        }

        // Clean up combined skills
        String combinedSkills = rawSkills;
        if (!technicalSkills.isBlank() && !softSkills.isBlank()) {
            combinedSkills = technicalSkills + ", " + softSkills;
        } else if (!technicalSkills.isBlank()) {
            combinedSkills = technicalSkills;
        }

        ParsedResumeData data = ParsedResumeData.builder()
                .resume(resume)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .linkedin(linkedin)
                .github(github)
                .portfolio(portfolio)
                .summary(summary)
                .skills(combinedSkills)
                .technicalSkills(technicalSkills)
                .softSkills(softSkills)
                .workExperience(experience)
                .education(education)
                .projects(projects)
                .certifications(certifications)
                .achievements(achievements)
                .rawText(extractedText)
                .build();

        return parsedResumeDataRepository.save(data);
    }

    public ParsedResumeData updateParsedData(Long resumeId, UpdateParsedResumeRequest req) {
        ParsedResumeData data = getParsedDataByResumeId(resumeId);

        if (req.getFullName() != null) data.setFullName(req.getFullName());
        if (req.getEmail() != null) data.setEmail(req.getEmail());
        if (req.getPhone() != null) data.setPhone(req.getPhone());
        if (req.getLinkedin() != null) data.setLinkedin(req.getLinkedin());
        if (req.getGithub() != null) data.setGithub(req.getGithub());
        if (req.getPortfolio() != null) data.setPortfolio(req.getPortfolio());
        if (req.getSummary() != null) data.setSummary(req.getSummary());
        if (req.getTechnicalSkills() != null) data.setTechnicalSkills(req.getTechnicalSkills());
        if (req.getSoftSkills() != null) data.setSoftSkills(req.getSoftSkills());
        if (req.getSkills() != null) data.setSkills(req.getSkills());
        if (req.getWorkExperience() != null) data.setWorkExperience(req.getWorkExperience());
        if (req.getEducation() != null) data.setEducation(req.getEducation());
        if (req.getProjects() != null) data.setProjects(req.getProjects());
        if (req.getCertifications() != null) data.setCertifications(req.getCertifications());
        if (req.getAchievements() != null) data.setAchievements(req.getAchievements());

        return parsedResumeDataRepository.save(data);
    }

    public String extractText(String filePath, String fileType) {
        try {
            if ("PDF".equalsIgnoreCase(fileType)) {
                return extractFromPdf(filePath);
            } else if ("DOCX".equalsIgnoreCase(fileType)) {
                return extractFromDocx(filePath);
            } else {
                throw new ApiException("Unsupported file type: " + fileType + ". Please upload PDF or DOCX.", HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to read file from path: {}", filePath, e);
            throw new ApiException("Failed to extract text from document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractFromPdf(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new ApiException("The uploaded PDF file is missing or empty.", HttpStatus.BAD_REQUEST);
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new ApiException("The uploaded PDF file is password protected.", HttpStatus.BAD_REQUEST);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return normalizeText(text);
        } catch (Exception e) {
            if (e instanceof ApiException) throw (ApiException) e;
            throw new ApiException("Could not parse PDF. File might be corrupted or image-only scanned: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private String extractFromDocx(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new ApiException("The uploaded DOCX file is missing or empty.", HttpStatus.BAD_REQUEST);
        }

        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String pText = paragraph.getText();
                if (pText != null && !pText.isBlank()) {
                    sb.append(pText.trim()).append("\n");
                }
            }

            // Also extract text inside tables (resumes often use 2-column tables)
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cText = cell.getText();
                        if (cText != null && !cText.isBlank()) {
                            sb.append(cText.trim()).append(" ");
                        }
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            throw new ApiException("Could not parse DOCX document: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return normalizeText(sb.toString());
    }

    private String normalizeText(String raw) {
        if (raw == null) return "";
        return raw.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F]", "")
                .replaceAll("\\t", " ")
                .trim();
    }

    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private String extractPortfolio(String text, String linkedin, String github) {
        Matcher matcher = PORTFOLIO_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            if ((linkedin == null || !candidate.contains("linkedin")) &&
                (github == null || !candidate.contains("github"))) {
                return candidate;
            }
        }
        return null;
    }

    private String guessFullName(String text) {
        String[] lines = text.split("\n");
        Pattern forbidden = Pattern.compile("(?i)(resume|curriculum|vitae|page|email|phone|contact|address|summary|objective|linkedin|github)");

        for (int i = 0; i < Math.min(lines.length, 12); i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() > 40 || line.length() < 3) continue;
            if (line.contains("@") || line.matches(".*\\d{4}.*")) continue;
            if (forbidden.matcher(line).find()) continue;
            // Likely a name: words starting with uppercase letters or all caps
            if (line.matches("^[a-zA-Z\\s.'-]{3,40}$")) {
                return line;
            }
        }
        return null;
    }

    private Map<String, String> segmentSections(String text) {
        Map<String, String> map = new HashMap<>();
        String[] lines = text.split("\n");

        String currentSection = "header";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            String detected = detectSectionHeader(trimmed);

            if (detected != null) {
                if (!currentSection.equals("header") && currentContent.length() > 0) {
                    map.put(currentSection, currentContent.toString().trim());
                }
                currentSection = detected;
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }

        if (currentContent.length() > 0) {
            map.put(currentSection, currentContent.toString().trim());
        }

        return map;
    }

    private String detectSectionHeader(String line) {
        if (line.length() > 45 || line.length() < 3) return null;
        String lower = line.toLowerCase();

        if (lower.matches(".*\\b(summary|professional summary|profile|about me|executive summary|career objective)\\b.*")) return "summary";
        if (lower.matches(".*\\b(technical skills|skills|technologies|tools & technologies|core competencies)\\b.*")) return "skills";
        if (lower.matches(".*\\b(work experience|experience|employment history|professional experience|career history)\\b.*")) return "experience";
        if (lower.matches(".*\\b(education|academic background|qualifications|degrees)\\b.*")) return "education";
        if (lower.matches(".*\\b(projects|personal projects|key projects|academic projects)\\b.*")) return "projects";
        if (lower.matches(".*\\b(certifications|certificates|licenses|courses)\\b.*")) return "certifications";
        if (lower.matches(".*\\b(achievements|awards|honors|publications|extracurricular)\\b.*")) return "achievements";

        return null;
    }

    private ParsedResumeData enhanceWithAi(String resumeText) {
        String prompt = """
                Extract structured resume information from this resume text.
                Return ONLY valid JSON (no markdown, no code fences):
                {
                  "fullName": "Candidate full name or null",
                  "email": "Email address or null",
                  "phone": "Phone number or null",
                  "linkedin": "LinkedIn URL or null",
                  "github": "GitHub URL or null",
                  "portfolio": "Portfolio or website URL or null",
                  "summary": "Professional summary or objective statement",
                  "technicalSkills": "Comma-separated list of technical skills (languages, frameworks, databases, tools)",
                  "softSkills": "Comma-separated list of soft skills (e.g. leadership, communication, problem solving)",
                  "workExperience": "Clean formatted text of work experience with companies, titles, dates and bullet points",
                  "education": "Clean formatted text of degrees, institutions and dates",
                  "projects": "Clean formatted text of key projects with technologies and descriptions",
                  "certifications": "Comma-separated or newline-separated certifications",
                  "achievements": "Key achievements or awards"
                }

                Resume text:
                """ + (resumeText.length() > 6000 ? resumeText.substring(0, 6000) : resumeText);

        try {
            String raw = geminiService.generateContent(prompt);
            String clean = raw.trim();
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode node = objectMapper.readTree(clean);
            return ParsedResumeData.builder()
                    .fullName(node.path("fullName").asText(null))
                    .email(node.path("email").asText(null))
                    .phone(node.path("phone").asText(null))
                    .linkedin(node.path("linkedin").asText(null))
                    .github(node.path("github").asText(null))
                    .portfolio(node.path("portfolio").asText(null))
                    .summary(node.path("summary").asText(null))
                    .technicalSkills(node.path("technicalSkills").asText(null))
                    .softSkills(node.path("softSkills").asText(null))
                    .workExperience(node.path("workExperience").asText(null))
                    .education(node.path("education").asText(null))
                    .projects(node.path("projects").asText(null))
                    .certifications(node.path("certifications").asText(null))
                    .achievements(node.path("achievements").asText(null))
                    .build();
        } catch (Exception e) {
            log.warn("Gemini parse enhancement failed: {}", e.getMessage());
            return null;
        }
    }

    public ParsedResumeData getParsedDataByResumeId(Long resumeId) {
        return parsedResumeDataRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ApiException("Parsed data not found for this resume", HttpStatus.NOT_FOUND));
    }
}
