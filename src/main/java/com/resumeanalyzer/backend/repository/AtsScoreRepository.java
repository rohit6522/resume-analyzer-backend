package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.entity.AtsScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AtsScoreRepository extends JpaRepository<AtsScore, Long> {
    Optional<AtsScore> findByResumeId(Long resumeId);
}