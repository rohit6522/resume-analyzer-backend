package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final GeminiService geminiService;

    @GetMapping("/api/health")
    public String healthCheck() {
        return "Backend is running successfully!";
    }

}