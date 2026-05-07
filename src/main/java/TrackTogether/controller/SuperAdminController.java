package TrackTogether.controller;

import TrackTogether.service.SuperAdminService;
import TrackTogether.service.SystemSettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("super_admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final SystemSettingsService systemSettingsService;

    public SuperAdminController(SuperAdminService superAdminService,
                                SystemSettingsService systemSettingsService) {
        this.superAdminService = superAdminService;
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String home(){
        return "superAdminDashboard";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String getAllUsers(Model model){

        model.addAttribute("users",superAdminService.findAllWithRole());

        return "allUsers";
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('MODERATOR')")
    public String getAllActivities(Model model){

        model.addAttribute("activities", superAdminService.findAllActivities());

        return "allActivities";
    }

    // Superadmin can change platform-wide settings from this page
    @GetMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String systemSettings(Model model) {
        model.addAttribute("settings", systemSettingsService.getSettings());
        return "system-settings";
    }

    @PostMapping("/settings/travelgroup-approval")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String updateTravelGroupApproval(@RequestParam(required = false) boolean enabled,
                                            RedirectAttributes redirectAttributes) {
        // The checkbox sends the new value, then we show a small confirmation toast.
        systemSettingsService.updateTravelGroupJoinApproval(enabled);

        redirectAttributes.addFlashAttribute("toastType", "success");
        redirectAttributes.addFlashAttribute(
                "toastMessage",
                enabled
                        ? "Travel group join approval is now enabled."
                        : "Travel groups now use direct join."
        );

        return "redirect:/super_admin/settings";
    }

    @GetMapping("user/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String userManagement(@PathVariable UUID id, Model model){

        model.addAttribute("user",superAdminService.findByIdWithRole(id));
        return "userManagement";
    }
}
