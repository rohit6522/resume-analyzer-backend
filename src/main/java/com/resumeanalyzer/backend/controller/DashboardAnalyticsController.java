package com.resumeanalyzer.backend.controller;

import com.resumeanalyzer.backend.dto.CareerDashboardResponse;
import com.resumeanalyzer.backend.service.DashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<CareerDashboardResponse> getAnalytics(Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getDashboardAnalytics(authentication.getName()));
    }
}

