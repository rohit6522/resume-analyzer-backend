package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.entity.JobApplication;
import com.resumeanalyzer.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByUserOrderByAppliedDateDesc(User user);
    Optional<JobApplication> findByIdAndUser(Long id, User user);
    long countByUser(User user);
    long countByUserAndStatus(User user, String status);
}

