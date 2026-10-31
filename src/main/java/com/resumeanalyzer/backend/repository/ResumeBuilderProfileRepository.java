package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.entity.ResumeBuilderProfile;
import com.resumeanalyzer.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeBuilderProfileRepository extends JpaRepository<ResumeBuilderProfile, Long> {
    List<ResumeBuilderProfile> findByUserOrderByUpdatedAtDesc(User user);
    Optional<ResumeBuilderProfile> findByIdAndUser(Long id, User user);
}

