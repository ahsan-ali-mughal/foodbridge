package com.foodbridge.admin.controller;

import com.foodbridge.admin.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("pendingNgos", dashboardService.getPendingNgos());
        model.addAttribute("activity", dashboardService.getRecentActivity(25));
        return "dashboard";
    }

    @PostMapping("/ngos/{userId}/approve")
    public String approve(@PathVariable Long userId) {
        dashboardService.approveNgo(userId);
        return "redirect:/dashboard";
    }

    @PostMapping("/ngos/{userId}/reject")
    public String reject(@PathVariable Long userId) {
        dashboardService.rejectNgo(userId);
        return "redirect:/dashboard";
    }
}
