package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
    List<JobMatch> findByResumeIdOrderByCreatedAtDesc(Long resumeId);
}