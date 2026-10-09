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

    private Integer overallScore;
    private Integer formattingScore;
    private Integer keywordScore;
    private Integer grammarScore;
    private Integer projectsScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer skillsScore;

    @Column(columnDefinition = "TEXT")
    private String strengths; // comma or newline separated

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String dateConsistencyNotes;

    @Column(updatable = false)
    private LocalDateTime generatedAt;


    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}