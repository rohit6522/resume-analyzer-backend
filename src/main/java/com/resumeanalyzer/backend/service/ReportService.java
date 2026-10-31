package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.entity.AtsScore;
import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.AtsScoreRepository;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AtsScoreRepository atsScoreRepository;
    private final ParsedResumeDataRepository parsedResumeDataRepository;

    private static final float MARGIN = 45;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    public byte[] generateReport(Resume resume) {

        AtsScore atsScore = atsScoreRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ApiException("Generate an ATS score before downloading a report", HttpStatus.BAD_REQUEST));

        ParsedResumeData parsedData = parsedResumeDataRepository.findByResumeId(resume.getId())
                .orElse(null);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header Title
                y = writeText(stream, fontBold, 20, MARGIN, y, "ResumeAI — Intelligence & ATS Analysis Report");
                y -= 4;
                y = writeText(stream, fontRegular, 9, MARGIN, y, "Generated on " +
                        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + " | Target: " +
                        (resume.getTargetRole() != null ? resume.getTargetRole() : "General"));
                y -= 16;

                // Candidate info block
                if (parsedData != null) {
                    y = writeText(stream, fontBold, 11, MARGIN, y, "Candidate Details:");
                    y -= 14;
                    String name = orNotFound(parsedData.getFullName());
                    String email = orNotFound(parsedData.getEmail());
                    String phone = orNotFound(parsedData.getPhone());
                    y = writeText(stream, fontRegular, 9, MARGIN, y, "Name: " + name + "   |   Email: " + email + "   |   Phone: " + phone);
                    y -= 18;
                }

                // Overall score banner
                int overall = atsScore.getOverallScore() != null ? atsScore.getOverallScore() : 0;
                String rating = overall >= 80 ? "EXCELLENT" : (overall >= 70 ? "COMPETITIVE" : "NEEDS IMPROVEMENT");
                y = writeText(stream, fontBold, 13, MARGIN, y, "Overall ATS Score: " + overall + "/100  (" + rating + ")");
                y -= 16;

                // 7 Core Transparent Criteria
                y = writeText(stream, fontBold, 11, MARGIN, y, "Transparent Criteria Breakdown (Out of 100):");
                y -= 14;
                y = writeScoreLine(stream, fontRegular, y, "1. Skills & Keyword Relevance", atsScore.getSkillsScore());
                y = writeScoreLine(stream, fontRegular, y, "2. Experience Quality & Depth", atsScore.getExperienceScore());
                y = writeScoreLine(stream, fontRegular, y, "3. Project Relevance & Scope", atsScore.getProjectsScore());
                y = writeScoreLine(stream, fontRegular, y, "4. Education & Certifications", atsScore.getEducationScore());
                y = writeScoreLine(stream, fontRegular, y, "5. Resume Formatting & Layout", atsScore.getFormattingScore());
                y = writeScoreLine(stream, fontRegular, y, "6. Content Quality & Grammar", atsScore.getContentQualityScore());
                y = writeScoreLine(stream, fontRegular, y, "7. ATS Machine Compatibility", atsScore.getAtsCompatibilityScore());
                y -= 10;

                // Action Verbs & Metrics
                if (atsScore.getActionVerbsFound() != null) {
                    y = writeSection(stream, fontBold, fontRegular, y, "Action Verbs Audit",
                            "Detected Power Verbs: " + atsScore.getActionVerbsFound() +
                            "\nRecommended to Add: " + (atsScore.getActionVerbsMissing() != null ? atsScore.getActionVerbsMissing() : "N/A"));
                }

                // Strengths
                y = writeSection(stream, fontBold, fontRegular, y, "Key Strengths", atsScore.getStrengths());
                // Weaknesses
                y = writeSection(stream, fontBold, fontRegular, y, "Identified Weaknesses", atsScore.getWeaknesses());
                // Suggestions
                y = writeSection(stream, fontBold, fontRegular, y, "Actionable Improvements", atsScore.getSuggestions());

                // Disclaimer
                y -= 8;
                writeText(stream, fontOblique, 8, MARGIN, y,
                        "Disclaimer: " + (atsScore.getEvaluationDisclaimer() != null ? atsScore.getEvaluationDisclaimer() :
                        "This ATS score is an estimated evaluation based on transparent recruitment heuristics, not proprietary algorithms."));
            }

            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new ApiException("Failed to generate report: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private float writeText(PDPageContentStream stream, PDFont font, float size, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(cleanTextForPdf(text));
        stream.endText();
        return y;
    }

    private float writeScoreLine(PDPageContentStream stream, PDFont font, float y, String label, Integer score) throws IOException {
        writeText(stream, font, 9, MARGIN + 10, y, label + ": " + (score != null ? score : "N/A") + "/100");
        return y - 13;
    }

    private float writeSection(PDPageContentStream stream, PDFont fontBold, PDFont fontRegular, float y, String title, String content) throws IOException {
        if (content == null || content.isBlank() || y < MARGIN + 40) {
            return y;
        }

        y = writeText(stream, fontBold, 10, MARGIN, y, title);
        y -= 13;

        List<String> lines = wrapText(content, fontRegular, 9, PAGE_WIDTH - (2 * MARGIN));
        for (String line : lines) {
            if (y < MARGIN + 25) break; // Don't overflow bottom
            y = writeText(stream, fontRegular, 9, MARGIN + 5, y, line);
            y -= 12;
        }
        return y - 8;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : rawLine.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(cleanTextForPdf(candidate)) / 1000 * fontSize;
                if (width > maxWidth) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private String cleanTextForPdf(String text) {
        if (text == null) return "";
        // Replace non-ASCII and characters not supported by Helvetica
        return text.replaceAll("[^\\x00-\\x7F]", "")
                .replace("\t", " ");
    }

    private String orNotFound(String value) {
        return (value != null && !value.isBlank()) ? value : "Not specified";
    }
}
