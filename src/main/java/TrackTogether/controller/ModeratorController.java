package TrackTogether.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorController {

    @GetMapping("/moderator")
    public String dashboard() {
        return "moderator";
    }

    @GetMapping("/moderator/reports/{reportId}")
    public ModelAndView reportDetail(@PathVariable UUID reportId) {
        ModelAndView mav = new ModelAndView("report-detail");
        mav.addObject("reportId", reportId);
        return mav;
    }
}