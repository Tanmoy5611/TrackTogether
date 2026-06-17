package TrackTogether.controller;

import TrackTogether.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService){
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String dashboard(){
        return "superAdminDashboard";
    }

    @GetMapping("/{id}")
    public String getMember(@PathVariable final String id, Model model){
        model.addAttribute("user",adminService.findByOriginalId(id));
        model.addAttribute("userType","Super Admin");
        return "userOverview";
    }

}