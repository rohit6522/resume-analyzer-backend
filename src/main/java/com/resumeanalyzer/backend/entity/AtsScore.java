package com.resumeanalyzer.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ats_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    private Integer overallScore; // 0-100

    // 7 Core Transparent Evaluation Criteria
    private Integer skillsScore;             // 1. Skills and keyword relevance
    private Integer experienceScore;         // 2. Experience quality
    private Integer projectsScore;           // 3. Project relevance
    private Integer educationScore;          // 4. Education and certifications
    private Integer formattingScore;         // 5. Resume formatting
    private Integer contentQualityScore;     // 6. Content quality
    private Integer atsCompatibilityScore;   // 7. ATS compatibility & structure

    // Additional diagnostic sub-scores
    private Integer keywordScore;
    private Integer grammarScore;
    private Integer actionVerbsScore;
    private Integer quantifiableMetricsScore;

    @Column(columnDefinition = "TEXT")
    private String strengths; // newline-separated bullet points

    @Column(columnDefinition = "TEXT")
    private String weaknesses; // newline-separated bullet points

    @Column(columnDefinition = "TEXT")
    private String suggestions; // actionable recommendations

    @Column(columnDefinition = "TEXT")
    private String actionVerbsFound;

    @Column(columnDefinition = "TEXT")
    private String actionVerbsMissing;

    @Column(columnDefinition = "TEXT")
    private String quantifiableMetricsNotes;

    @Column(columnDefinition = "TEXT")
    private String missingSections;

    @Column(columnDefinition = "TEXT")
    private String dateConsistencyNotes;

    @Column(columnDefinition = "TEXT")
    private String evaluationDisclaimer;

    @Column(columnDefinition = "TEXT")
    private String sectionBreakdownJson;

    @Column(updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
        if (evaluationDisclaimer == null) {
            evaluationDisclaimer = "Estimated ATS score based on transparent industry heuristics and standard recruitment criteria, not proprietary algorithms.";
        }
    }
}
