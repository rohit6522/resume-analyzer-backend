package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.entity.ParsedResumeData;
import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.exception.ApiException;
import com.resumeanalyzer.backend.repository.ParsedResumeDataRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResumeParserService {

    private final ParsedResumeDataRepository parsedResumeDataRepository;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3,5}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}");

    private static final Pattern LINKEDIN_PATTERN =
            Pattern.compile("(https?://)?(www\\.)?linkedin\\.com/[a-zA-Z0-9\\-_/]+");

    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9\\-_/]+");

    public ParsedResumeData parseAndSave(Resume resume) {

        String extractedText = extractText(resume.getFilePath(), resume.getFileType());

        ParsedResumeData data = ParsedResumeData.builder()
                .resume(resume)
                .email(extractFirst(EMAIL_PATTERN, extractedText))
                .phone(extractFirst(PHONE_PATTERN, extractedText))
                .linkedin(extractFirst(LINKEDIN_PATTERN, extractedText))
                .github(extractFirst(GITHUB_PATTERN, extractedText))
                .fullName(guessFullName(extractedText))
                .rawText(extractedText)
                .build();

        return parsedResumeDataRepository.save(data);
    }

    private String extractText(String filePath, String fileType) {
        try {
            if ("PDF".equalsIgnoreCase(fileType)) {
                return extractFromPdf(filePath);
            } else if ("DOCX".equalsIgnoreCase(fileType)) {
                return extractFromDocx(filePath);
            } else {
                throw new ApiException("Unsupported file type for parsing", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to extract text: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractFromPdf(String filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new java.io.File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(String filePath) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String guessFullName(String text) {
        // Naive approach: first non-empty line is usually the name
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 50 && !trimmed.contains("@")) {
                return trimmed;
            }
        }
        return null;
    }

    public ParsedResumeData getParsedDataByResumeId(Long resumeId) {
        return parsedResumeDataRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ApiException("Parsed data not found for this resume", HttpStatus.NOT_FOUND));
    }
}