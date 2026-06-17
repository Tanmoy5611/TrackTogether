package TrackTogether.controller;

import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.service.MemberService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final MessageSource messageSource;

    public MemberController(MemberService memberService,
                            MessageSource messageSource){
        this.memberService = memberService;
        this.messageSource = messageSource;
    }

    @GetMapping("/{id}")
    public String getMember(@PathVariable final String id, Model model){
        model.addAttribute("user",memberService.findByOriginalIdOrThrow(id));
        model.addAttribute("userType","Member");
        model.addAttribute("canEditTravelPreferences", false);
        // Passing modes from Java keeps the Thymeleaf profile page simple
        model.addAttribute("transportModes", TransportMode.values());
        return "userOverview";
    }

    @GetMapping("/profile")
    public String getProfile(Model model){
        Member member = (Member) model.getAttribute("currentUser");
        model.addAttribute("user",member);
        model.addAttribute("userType","Member");
        model.addAttribute("canEditTravelPreferences", true);
        // Same list is needed when the logged in user edits preferences
        model.addAttribute("transportModes", TransportMode.values());
        return "userOverview";
    }

    @PostMapping("/profile/travel-preferences")
    public String updateTravelPreferences(@RequestParam TransportMode preferredTransportMode,
                                          @RequestParam String defaultDepartureLocation,
                                          @RequestParam(required = false) Double defaultLatitude,
                                          @RequestParam(required = false) Double defaultLongitude,
                                          RedirectAttributes redirectAttributes) {
        try {
            memberService.updateCurrentUserTravelPreferences(
                    preferredTransportMode,
                    defaultDepartureLocation,
                    defaultLatitude,
                    defaultLongitude
            );
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.member.travelPreferencesSaved"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        return "redirect:/member/profile";
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}