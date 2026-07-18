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
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AtsScoreRepository atsScoreRepository;
    private final ParsedResumeDataRepository parsedResumeDataRepository;

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();

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

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                // Title
                y = writeText(stream, fontBold, 22, MARGIN, y, "Resume Analysis Report");
                y -= 5;
                y = writeText(stream, fontRegular, 10, MARGIN, y, "Generated on " +
                        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
                y -= 20;

                // Candidate info
                if (parsedData != null) {
                    y = writeText(stream, fontBold, 13, MARGIN, y, "Candidate Information");
                    y -= 18;
                    y = writeText(stream, fontRegular, 10, MARGIN, y, "Name: " + orNotFound(parsedData.getFullName()));
                    y -= 14;
                    y = writeText(stream, fontRegular, 10, MARGIN, y, "Email: " + orNotFound(parsedData.getEmail()));
                    y -= 14;
                    y = writeText(stream, fontRegular, 10, MARGIN, y, "Phone: " + orNotFound(parsedData.getPhone()));
                    y -= 25;
                }

                // Overall score
                y = writeText(stream, fontBold, 13, MARGIN, y, "Overall ATS Score: " + atsScore.getOverallScore() + "/100");
                y -= 25;

                // Score breakdown
                y = writeText(stream, fontBold, 13, MARGIN, y, "Score Breakdown");
                y -= 18;
                y = writeScoreLine(stream, fontRegular, y, "Formatting", atsScore.getFormattingScore());
                y = writeScoreLine(stream, fontRegular, y, "Keywords", atsScore.getKeywordScore());
                y = writeScoreLine(stream, fontRegular, y, "Grammar", atsScore.getGrammarScore());
                y = writeScoreLine(stream, fontRegular, y, "Projects", atsScore.getProjectsScore());
                y = writeScoreLine(stream, fontRegular, y, "Experience", atsScore.getExperienceScore());
                y = writeScoreLine(stream, fontRegular, y, "Education", atsScore.getEducationScore());
                y = writeScoreLine(stream, fontRegular, y, "Skills", atsScore.getSkillsScore());
                y -= 15;

                // Strengths
                y = writeSection(stream, fontBold, fontRegular, y, "Strengths", atsScore.getStrengths());
                // Weaknesses
                y = writeSection(stream, fontBold, fontRegular, y, "Weaknesses", atsScore.getWeaknesses());
                // Suggestions
                y = writeSection(stream, fontBold, fontRegular, y, "Suggestions for Improvement", atsScore.getSuggestions());
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
        stream.showText(text);
        stream.endText();
        return y;
    }

    private float writeScoreLine(PDPageContentStream stream, PDFont font, float y, String label, Integer score) throws IOException {
        writeText(stream, font, 10, MARGIN, y, label + ": " + score + "/100");
        return y - 14;
    }

    private float writeSection(PDPageContentStream stream, PDFont fontBold, PDFont fontRegular, float y, String title, String content) throws IOException {
        y = writeText(stream, fontBold, 12, MARGIN, y, title);
        y -= 16;

        if (content == null || content.isBlank()) {
            return y - 10;
        }

        List<String> lines = wrapText(content, fontRegular, 10, PAGE_WIDTH - (2 * MARGIN));
        for (String line : lines) {
            y = writeText(stream, fontRegular, 10, MARGIN, y, line);
            y -= 13;
        }
        return y - 12;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        for (String rawLine : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : rawLine.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
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

    private String orNotFound(String value) {
        return value == null || value.isBlank() ? "Not found" : value;
    }
}