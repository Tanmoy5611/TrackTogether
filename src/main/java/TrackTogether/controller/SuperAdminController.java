package TrackTogether.controller;

import TrackTogether.service.SuperAdminService;
import TrackTogether.service.SystemSettingsService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    public SuperAdminController(SuperAdminService superAdminService,
                                SystemSettingsService systemSettingsService,
                                MessageSource messageSource) {
        this.superAdminService = superAdminService;
        this.systemSettingsService = systemSettingsService;
        this.messageSource = messageSource;
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
        systemSettingsService.updateTravelGroupJoinApproval(enabled);

        redirectAttributes.addFlashAttribute("toastType", "success");
        redirectAttributes.addFlashAttribute(
                "toastMessage",
                enabled
                        ? message("flash.settings.joinApprovalEnabled")
                        : message("flash.settings.directJoinEnabled")
        );

        return "redirect:/super_admin/settings";
    }

    @GetMapping("user/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String userManagement(@PathVariable UUID id, Model model){

        model.addAttribute("user",superAdminService.findByIdWithRole(id));
        model.addAttribute("lastSuperAdmin", superAdminService.isLastSuperAdmin(id));
        return "userManagement";
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}