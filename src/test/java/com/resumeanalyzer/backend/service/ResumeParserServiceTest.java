package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.UpdateParsedResumeRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeParserServiceTest {

    @Mock
    private ParsedResumeDataRepository parsedResumeDataRepository;

    @Mock
    private GeminiService geminiService;

    private ResumeParserService resumeParserService;

    @BeforeEach
    void setUp() {
        resumeParserService = new ResumeParserService(parsedResumeDataRepository, geminiService);
    }

    @Test
    void testUpdateParsedData() {
        Long resumeId = 100L;
        Resume resume = Resume.builder().id(resumeId).build();
        ParsedResumeData existing = ParsedResumeData.builder()
                .id(1L)
                .resume(resume)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .build();

        when(parsedResumeDataRepository.findByResumeId(resumeId)).thenReturn(Optional.of(existing));
        when(parsedResumeDataRepository.save(any(ParsedResumeData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParsedResumeRequest update = UpdateParsedResumeRequest.builder()
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .technicalSkills("Java, Spring Boot, React")
                .build();

        ParsedResumeData updated = resumeParserService.updateParsedData(resumeId, update);

        assertNotNull(updated);
        assertEquals("Jane Smith", updated.getFullName());
        assertEquals("jane.smith@example.com", updated.getEmail());
        assertEquals("Java, Spring Boot, React", updated.getTechnicalSkills());
        verify(parsedResumeDataRepository, times(1)).save(existing);
    }
}

