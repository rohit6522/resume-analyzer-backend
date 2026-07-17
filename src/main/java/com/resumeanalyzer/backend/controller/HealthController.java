package com.resumeanalyzer.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String healthCheck() {
        return "Backend is running successfully!";
    }


    @GetMapping("/api/protected/test")
    public String protectedTest() {
        return "You are authenticated! JWT is working correctly.";
    }
}