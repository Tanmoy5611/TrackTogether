package TrackTogether.controller;

import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.service.ActivityService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.view.TravelGroupForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        Set<UUID> ownedGroupIds = travelGroupService.getOwnedGroupIds(groups);
        Map<UUID, Long> memberCounts = travelGroupService.getMemberCounts(groups);

        model.addAttribute("groups", groups);
        model.addAttribute("joinedGroupIds", joinedGroupIds);
        model.addAttribute("ownedGroupIds", ownedGroupIds);
        model.addAttribute("memberCounts", memberCounts);
        model.addAttribute("pendingJoinRequestGroupIds", travelGroupService.getPendingJoinRequestGroupIds(groups));
        model.addAttribute("rejectedJoinRequestGroupIds", travelGroupService.getRejectedJoinRequestGroupIds(groups));
        model.addAttribute("pendingJoinRequestCounts", travelGroupService.getPendingJoinRequestCounts(groups));

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

    // MVC Request to join group
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId,
                                  @RequestParam(required = false) String redirectTo,
                                  RedirectAttributes redirectAttributes) {

        try {
            travelGroupService.requestToJoinTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Join request sent. The group creator can accept or reject it.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

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

    @PostMapping("/requests/{requestId}/accept")
    public String acceptJoinRequest(@PathVariable Integer requestId,
                                    @RequestParam(required = false) String redirectTo) {
        travelGroupService.acceptJoinRequest(requestId);

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/travelgroups";
    }

    @PostMapping("/requests/{requestId}/reject")
    public String rejectJoinRequest(@PathVariable Integer requestId,
                                    @RequestParam(required = false) String redirectTo) {
        travelGroupService.rejectJoinRequest(requestId);

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/travelgroups";
    }


    @GetMapping("/{groupId}")
    public String showTravelGroupDetails(@PathVariable UUID groupId, Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        List<TravelGroupMember> groupMembers = travelGroupService.getMembersForGroup(group);
        boolean joined = travelGroupService.isCurrentUserMember(group);
        boolean owner = travelGroupService.isCurrentUserOwner(group);
        JoinRequestStatus joinRequestStatus = travelGroupService.getCurrentUserJoinRequestStatus(group);
        long memberCount = groupMembers.size();

        model.addAttribute("group", group);
        model.addAttribute("groupMembers", groupMembers);
        model.addAttribute("pendingJoinRequests", owner ? travelGroupService.getPendingJoinRequestsForGroup(group) : List.of());
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("remainingSpots", Math.max(group.getMaxMembers() - memberCount, 0));
        model.addAttribute("isJoined", joined);
        model.addAttribute("isOwner", owner);
        model.addAttribute("joinRequestStatus", joinRequestStatus);
        model.addAttribute("hasPendingJoinRequest", joinRequestStatus == JoinRequestStatus.PENDING);
        model.addAttribute("hasRejectedJoinRequest", joinRequestStatus == JoinRequestStatus.REJECTED);

        return "travelgroup-detail";
    }
}