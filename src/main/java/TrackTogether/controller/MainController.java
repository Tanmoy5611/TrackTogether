package TrackTogether.controller;

import TrackTogether.dto.HomePageView;
import TrackTogether.service.HomePageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // Service responsible for preparing all data shown on the home page
    private final HomePageService homePageService;

    public MainController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Ask the service to prepare the homepage data
        HomePageView homePage = homePageService.buildHomePage();

        // Expose one clean object to the Thymeleaf template
        model.addAttribute("homePage", homePage);

        return "index";
    }
}
