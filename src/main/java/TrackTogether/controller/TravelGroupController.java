package TrackTogether.controller;

import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Activity;
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
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/travelgroups")
public class TravelGroupController {

    private static final DateTimeFormatter DATE_TIME_INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final TravelGroupService travelGroupService;
    private final ActivityService activityService;

    public TravelGroupController(TravelGroupService travelGroupService,
                                 ActivityService activityService) {
        // Spring injects the services that contain the real business rules
        this.travelGroupService = travelGroupService;
        this.activityService = activityService;
    }

    // Shows the travel group overview page with one page view object
    @GetMapping
    public String showAllTravelGroups(Model model) {
        model.addAttribute("page", travelGroupService.buildTravelGroupsPage());
        return "travelgroups";
    }

    // Opens the create page and pre-selects an activity when it comes from an activity page
    @GetMapping("/create")
    public String showTravelGroupCreateForm(@RequestParam(required = false) UUID activityId,
                                            Model model) {
        TravelGroupForm form = new TravelGroupForm();

        if (activityId != null) {
            form.setActivityId(activityId);
        }

        populateCreateTravelGroupModel(model, form);

        return "create-travelgroup";
    }

    // Handles create submit and returns the form again when validation fails
    @PostMapping("/create")
    public String createTravelGroup(@Valid @ModelAttribute TravelGroupForm form,
                                    BindingResult bindingResult,
                                    Model model) {

        if (bindingResult.hasErrors()) {
            populateCreateTravelGroupModel(model, form);
            return "create-travelgroup";
        }

        try {
            // The service creates the group, owner membership, and conversation together
            TravelGroup group = travelGroupService.createTravelGroup(
                    form.getActivityId(),
                    form.getMaxMembers(),
                    form.getLocation(),
                    form.getMode(),
                    form.getDepartureTime()
            );

            return "redirect:/activities/" + group.getActivity().getId();
        } catch (ResponseStatusException exception) {
            // Service validation errors are shown next to the correct form field
            rejectTravelGroupFormValue(bindingResult, exception);
            populateCreateTravelGroupModel(model, form);
            return "create-travelgroup";
        }
    }

    // Opens the edit page only for the current owner of the group
    @GetMapping("/{groupId}/edit")
    public String showTravelGroupEditForm(@PathVariable UUID groupId,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);

        if (!travelGroupService.canCurrentUserEditTravelGroup(group)) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute(
                    "toastMessage",
                    "Only the group owner can edit this travel group."
            );
            return "redirect:/travelgroups/" + groupId;
        }

        // Copy the saved entity values into the form object for Thymeleaf
        TravelGroupForm form = new TravelGroupForm();
        form.setActivityId(group.getActivity().getId());
        form.setMaxMembers(group.getMaxMembers());
        form.setLocation(group.getLocation());
        form.setDepartureTime(group.getDepartureTime());
        form.setMode(group.getTransportMode());

        populateEditTravelGroupModel(model, group, form);
        return "edit-travelgroup";
    }

    // Saves owner edits and keeps the user on the edit page if something is invalid
    @PostMapping("/{groupId}/edit")
    public String updateTravelGroup(@PathVariable UUID groupId,
                                    @Valid @ModelAttribute("travelGroupForm") TravelGroupForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);

        if (bindingResult.hasErrors()) {
            populateEditTravelGroupModel(model, group, form);
            return "edit-travelgroup";
        }

        try {
            // The service checks ownership and validates the new group details
            TravelGroup updatedGroup = travelGroupService.updateTravelGroup(
                    groupId,
                    form.getMaxMembers(),
                    form.getLocation(),
                    form.getMode(),
                    form.getDepartureTime()
            );

            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Travel group details updated.");
            return "redirect:/travelgroups/" + updatedGroup.getGroupId();
        } catch (ResponseStatusException exception) {
            // Keep the entered values and show the validation message on the form
            rejectTravelGroupFormValue(bindingResult, exception);
            populateEditTravelGroupModel(model, group, form);
            return "edit-travelgroup";
        }
    }

    // Lets the current user join or request to join depending on the system setting
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId,
                                  @RequestParam(required = false) String redirectTo,
                                  RedirectAttributes redirectAttributes) {

        try {
            boolean joinApprovalRequired = travelGroupService.isJoinApprovalRequired();
            travelGroupService.joinTravelGroup(groupId);
            // Message is different because approval mode does not immediately add the member
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute(
                    "toastMessage",
                    joinApprovalRequired
                            ? "Join request sent. The group owner can accept or reject it."
                            : "Joined travel group."
            );
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        // Some pages pass their own return URL, for example the activity detail page
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        return "redirect:/travelgroups/" + group.getGroupId();
    }

    // Lets a normal member leave. Owners are blocked in the service
    @PostMapping("/{groupId}/leave")
    public String leaveTravelGroup(@PathVariable UUID groupId,
                                   @RequestParam(required = false) String redirectTo,
                                   RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        UUID activityId = group.getActivity().getId();

        try {
            // Owner restrictions and empty-group cleanup happen inside the service.
            travelGroupService.leaveTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "You left the travel group.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/activities/" + activityId;
    }

    // Deletes a group only when the current owner is the only remaining member
    @PostMapping("/{groupId}/delete")
    public String deleteOwnedTravelGroup(@PathVariable UUID groupId,
                                         @RequestParam(required = false) String redirectTo,
                                         RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        UUID activityId = group.getActivity().getId();

        try {
            // Only owner-only groups can be deleted from this action
            travelGroupService.deleteOwnedTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Travel group deleted.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());

            if (redirectTo != null && !redirectTo.isBlank()) {
                return "redirect:" + redirectTo;
            }

            return "redirect:/travelgroups/" + groupId;
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/activities/" + activityId;
    }

    // Transfers ownership from the current owner to another joined member
    @PostMapping("/{groupId}/transfer-ownership")
    public String transferOwnership(@PathVariable UUID groupId,
                                    @RequestParam UUID newOwnerId,
                                    @RequestParam(required = false) String redirectTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Ownership transfer is handled in the service so MVC and API use the same rules.
            travelGroupService.transferOwnership(groupId, newOwnerId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Travel group ownership transferred.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/travelgroups/" + groupId;
    }

    // Accepts a pending join request from the detail page
    @PostMapping("/requests/{requestId}/accept")
    public String acceptJoinRequest(@PathVariable Integer requestId,
                                    @RequestParam(required = false) String redirectTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.acceptJoinRequest(requestId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Join request accepted.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/travelgroups";
    }

    // Rejects a pending join request from the detail page
    @PostMapping("/requests/{requestId}/reject")
    public String rejectJoinRequest(@PathVariable Integer requestId,
                                    @RequestParam(required = false) String redirectTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.rejectJoinRequest(requestId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", "Join request rejected.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }

        return "redirect:/travelgroups";
    }

    // Shows one travel group with member status, owner actions, and join requests
    @GetMapping("/{groupId}")
    public String showTravelGroupDetails(@PathVariable UUID groupId, Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        List<TravelGroupMember> groupMembers = travelGroupService.getMembersForGroup(group);
        boolean joined = travelGroupService.isCurrentUserMember(group);
        boolean owner = travelGroupService.isCurrentUserOwner(group);
        JoinRequestStatus joinRequestStatus = travelGroupService.getCurrentUserJoinRequestStatus(group);
        boolean joinApprovalRequired = travelGroupService.isJoinApprovalRequired();
        long memberCount = groupMembers.size();

        // These values drive the detail page badges and available buttons
        model.addAttribute("group", group);
        model.addAttribute("groupMembers", groupMembers);
        model.addAttribute("pendingJoinRequests", travelGroupService.getVisiblePendingRequests(group));
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("remainingSpots", Math.max(group.getMaxMembers() - memberCount, 0));
        model.addAttribute("isJoined", joined);
        model.addAttribute("isOwner", owner);
        model.addAttribute("ownerCanDeleteGroup", travelGroupService.canCurrentUserDeleteTravelGroup(group));
        model.addAttribute("joinApprovalRequired", joinApprovalRequired);
        model.addAttribute("ownerCannotLeaveMessage", travelGroupService.getOwnerCannotLeaveMessage());
        model.addAttribute("joinRequestStatus", joinRequestStatus);
        model.addAttribute("hasPendingJoinRequest", joinApprovalRequired && joinRequestStatus == JoinRequestStatus.PENDING);
        model.addAttribute("hasRejectedJoinRequest", joinApprovalRequired && joinRequestStatus == JoinRequestStatus.REJECTED);

        return "travelgroup-detail";
    }

    // Fills all model values needed by the create form
    private void populateCreateTravelGroupModel(Model model, TravelGroupForm form) {
        // Same create model for first load and validation errors.
        List<Activity> activities = activityService.getAllActivities();
        model.addAttribute("travelGroupForm", form);
        model.addAttribute("activities", activities);
        model.addAttribute(
                "activityDepartureLimits",
                activities.stream().collect(Collectors.toMap(Activity::getId, this::formatActivityDateTime))
        );
        model.addAttribute("minDepartureTime", formatDateTime(LocalDateTime.now()));

        if (form.getActivityId() != null) {
            // Used by the map and max departure time when an activity is already selected.
            Activity selectedActivity = activityService.getActivityById(form.getActivityId());
            model.addAttribute("selectedActivity", selectedActivity);
            model.addAttribute("maxDepartureTime", formatActivityDateTime(selectedActivity));
        }
    }

    // Fills all model values needed by the edit form and owner tools
    private void populateEditTravelGroupModel(Model model, TravelGroup group, TravelGroupForm form) {
        // The edit page needs the current group and form values after validation errors
        model.addAttribute("group", group);
        model.addAttribute("travelGroupForm", form);
        model.addAttribute("memberCount", travelGroupService.getMemberCount(group));
        // Owner tools need only joined members, because ownership cannot go to an outsider
        model.addAttribute("ownershipTransferCandidates", travelGroupService.getOwnershipTransferCandidates(group));
        model.addAttribute("minDepartureTime", formatDateTime(LocalDateTime.now()));
        model.addAttribute("maxDepartureTime", formatActivityDateTime(group.getActivity()));
    }

    // Maps service validation messages to the form field that should show the error
    private void rejectTravelGroupFormValue(BindingResult bindingResult, ResponseStatusException exception) {
        String reason = exception.getReason();
        String field = "departureTime";

        if (reason != null && reason.startsWith("Maximum members")) {
            field = "maxMembers";
        } else if (reason != null && reason.startsWith("Location")) {
            field = "location";
        } else if (reason != null && reason.startsWith("Transport")) {
            field = "mode";
        } else if (reason != null && reason.startsWith("Activity")) {
            field = "activityId";
        }

        bindingResult.rejectValue(
                field,
                "travelGroupForm." + field + ".invalid",
                exception.getReason()
        );
    }

    // Converts an activity date and time into the HTML datetime-local format
    private String formatActivityDateTime(Activity activity) {
        if (activity.getDate() == null || activity.getTime() == null) {
            return "";
        }

        return formatDateTime(LocalDateTime.of(activity.getDate(), activity.getTime()));
    }

    // Formats LocalDateTime for datetime-local min, max, and values
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_INPUT_FORMATTER);
    }
}