package com.resumeanalyzer.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_resume_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedResumeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    private String fullName;
    private String email;
    private String phone;
    private String linkedin;
    private String github;
    private String portfolio;

    @Column(columnDefinition = "TEXT")
    private String skills; // comma-separated for now

    @Column(columnDefinition = "TEXT")
    private String rawText; // full extracted text, useful for AI later

    @Column(updatable = false)
    private LocalDateTime parsedAt;

    @PrePersist
    protected void onCreate() {
        parsedAt = LocalDateTime.now();
    }
}