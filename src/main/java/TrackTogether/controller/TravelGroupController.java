package TrackTogether.controller;

import TrackTogether.service.TravelGroupService;
import TrackTogether.view.TravelGroupForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/travelgroups")
public class TravelGroupController {

    private final TravelGroupService travelGroupService;

    public TravelGroupController(TravelGroupService travelGroupService) {
        this.travelGroupService = travelGroupService;
    }

    // Show all travel groups
    @GetMapping
    public String showAllTravelGroups(Model model) {

        model.addAttribute(
                "groups",
                travelGroupService.getAllTravelGroups()
        );

        return "travelgroups";
    }

    // Show create form
    @GetMapping("/create")
    public String showTravelGroupCreateForm(Model model) {

        model.addAttribute(
                "travelGroupForm",
                new TravelGroupForm()
        );

        return "create-travelgroup";
    }

    // Handle create form submit
    @PostMapping("/create")
    public String createTravelGroup(@Valid @ModelAttribute TravelGroupForm form,
                                    BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "create-travelgroup";
        }

        travelGroupService.createTravelGroup(
                form.getActivityId(),
                form.getMaxMembers(),
                form.getLocation(),
                form.getMode()
        );

        return "redirect:/travelgroups";
    }

    // MVC Join group
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId) {

        travelGroupService.joinTravelGroup(groupId);

        return "redirect:/travelgroups";
    }

    // MVC Leave group
    @PostMapping("/{groupId}/leave")
    public String leaveTravelGroup(@PathVariable UUID groupId) {

        travelGroupService.leaveTravelGroup(groupId);

        return "redirect:/travelgroups";
    }
}