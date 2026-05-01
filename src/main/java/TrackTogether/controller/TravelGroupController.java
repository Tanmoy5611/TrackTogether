package TrackTogether.controller;

import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.service.ActivityService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.view.TravelGroupForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/travelgroups")
public class TravelGroupController {

    private final TravelGroupService travelGroupService;
    private final ActivityService activityService;

    public TravelGroupController(TravelGroupService travelGroupService,
                                 ActivityService activityService) {
        this.travelGroupService = travelGroupService;
        this.activityService = activityService;
    }

    // Show all travel groups
    @GetMapping
    public String showAllTravelGroups(Model model) {
        List<TravelGroup> groups = travelGroupService.getAllTravelGroups();
        Set<UUID> joinedGroupIds = travelGroupService.getJoinedGroupIds(groups);
        Map<UUID, Long> memberCounts = travelGroupService.getMemberCounts(groups);

        model.addAttribute("groups", groups);
        model.addAttribute("joinedGroupIds", joinedGroupIds);
        model.addAttribute("memberCounts", memberCounts);

        return "travelgroups";
    }

    // Show create form
    @GetMapping("/create")
    public String showTravelGroupCreateForm(@RequestParam(required = false) UUID activityId,
                                            Model model) {
        TravelGroupForm form = new TravelGroupForm();

        if (activityId != null) {
            form.setActivityId(activityId);
            model.addAttribute("selectedActivity", activityService.getActivityById(activityId));
        }

        model.addAttribute("travelGroupForm", form);
        model.addAttribute("activities", activityService.getAllActivities());

        return "create-travelgroup";
    }

    // Handle create form submit
    @PostMapping("/create")
    public String createTravelGroup(@Valid @ModelAttribute TravelGroupForm form,
                                    BindingResult bindingResult,
                                    Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activities", activityService.getAllActivities());

            if (form.getActivityId() != null) {
                model.addAttribute("selectedActivity", activityService.getActivityById(form.getActivityId()));
            }

            return "create-travelgroup";
        }

        TravelGroup group = travelGroupService.createTravelGroup(
                form.getActivityId(),
                form.getMaxMembers(),
                form.getLocation(),
                form.getMode()
        );

        return "redirect:/activities/" + group.getActivity().getId();
    }

    // MVC Join group
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId,
                                  @RequestParam(required = false) String redirectTo) {

        travelGroupService.joinTravelGroup(groupId);

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        return "redirect:/travelgroups/" + group.getGroupId();
    }

    // MVC Leave group
    @PostMapping("/{groupId}/leave")
    public String leaveTravelGroup(@PathVariable UUID groupId,
                                   @RequestParam(required = false) String redirectTo) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        UUID activityId = group.getActivity().getId();

        travelGroupService.leaveTravelGroup(groupId);

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/activities/" + activityId;
    }

    @GetMapping("/{groupId}")
    public String showTravelGroupDetails(@PathVariable UUID groupId, Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        List<TravelGroupMember> groupMembers = travelGroupService.getMembersForGroup(group);
        boolean joined = travelGroupService.isCurrentUserMember(group);
        long memberCount = groupMembers.size();

        model.addAttribute("group", group);
        model.addAttribute("groupMembers", groupMembers);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("remainingSpots", Math.max(group.getMaxMembers() - memberCount, 0));
        model.addAttribute("isJoined", joined);

        return "travelgroup-detail";
    }
}