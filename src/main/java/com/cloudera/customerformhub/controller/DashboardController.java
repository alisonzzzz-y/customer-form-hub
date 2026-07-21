package com.cloudera.customerformhub.controller;

import com.cloudera.customerformhub.dto.DashboardStats;
import com.cloudera.customerformhub.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /api/dashboard/stats  → aggregated read-only metrics for the dashboard
    @GetMapping("/stats")
    public DashboardStats getStats() {
        return dashboardService.getStats();
    }
}