package TrackTogether.controller;

import TrackTogether.view.TravelGroupForm;
import TrackTogether.service.TravelGroupService;
import org.springframework.stereotype.Controller;
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

        model.addAttribute("groups", travelGroupService.getAllTravelGroups());

        return "travelgroups";
    }

    // Show create form
    @GetMapping("/create")
    public String showTravelGroupCreateForm(Model model) {

        //  bind empty form
        model.addAttribute("travelGroupForm", new TravelGroupForm());

        return "create-travelgroup";
    }

    // Handle create form submit
    @PostMapping("/create")
    public String createTravelGroup(@ModelAttribute TravelGroupForm form) {

        travelGroupService.createTravelGroup(
                form.getActivityId(),
                form.getMaxMembers(),
                form.getLocation(),
                form.getMode()
        );

        return "redirect:/travelgroups";
    }

    // Join group
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId,
                                  @RequestParam UUID memberId) {

        travelGroupService.joinTravelGroup(groupId, memberId);

        return "redirect:/travelgroups";
    }

    // Leave group
    @PostMapping("/{groupId}/leave")
    public String leaveTravelGroup(@PathVariable UUID groupId,
                                   @RequestParam UUID memberId) {

        travelGroupService.leaveTravelGroup(groupId, memberId);

        return "redirect:/travelgroups";
    }
}