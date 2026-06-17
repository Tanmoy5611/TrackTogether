package TrackTogether.controller;

import TrackTogether.domain.Member;
import TrackTogether.dto.analytics.Co2Period;
import TrackTogether.dto.analytics.Co2TimePointView;
import TrackTogether.service.AnalyticsService;
import TrackTogether.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService,
                               CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public String userAnalytics(Model model) {
        Member currentUser = currentUserService.getCurrentUser();
        model.addAttribute("analytics", analyticsService.getUserAnalytics(currentUser));
        return "userAnalytics";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("dashboard", analyticsService.getAdminAnalytics());
        return "analyticsDashboard";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseBody
    @GetMapping("/dashboard/co2")
    public List<Co2TimePointView> co2ThroughTime(
            @RequestParam(defaultValue = "MONTHLY") Co2Period period) {
        return analyticsService.getCo2ThroughTime(period);
    }
}