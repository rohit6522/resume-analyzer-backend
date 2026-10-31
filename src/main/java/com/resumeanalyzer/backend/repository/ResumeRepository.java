package com.resumeanalyzer.backend.repository;

import com.resumeanalyzer.backend.entity.Resume;
import com.resumeanalyzer.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserOrderByUploadedAtDesc(User user);

    Optional<Resume> findByUserAndActiveTrue(User user);

    List<Resume> findByUserIdOrderByUploadedAtDesc(Long userId);

    long countByUser(User user);
}
