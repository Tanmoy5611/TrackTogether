package TrackTogether.controller;

import TrackTogether.dto.HomePageView;
import TrackTogether.service.HomePageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final HomePageService homePageService;

    public MainController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof OidcUser)) {
            return "landing";
        }

        // Ask the service to prepare the homepage data
        HomePageView homePage = homePageService.buildHomePage();

        model.addAttribute("homePage", homePage);

        return "index";
    }
}