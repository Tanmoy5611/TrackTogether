package TrackTogether.controller;

import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Activity;
import TrackTogether.dto.TravelGroupActivityLogView;
import TrackTogether.service.ActivityService;
import TrackTogether.service.GoogleCalendarLinkService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.view.TravelGroupForm;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final GoogleCalendarLinkService googleCalendarLinkService;
    private final MessageSource messageSource;

    // Wires the travel group page controller with its service dependencies
    public TravelGroupController(TravelGroupService travelGroupService,
                                 ActivityService activityService,
                                 GoogleCalendarLinkService googleCalendarLinkService,
                                 MessageSource messageSource) {
        this.travelGroupService = travelGroupService;
        this.activityService = activityService;
        this.googleCalendarLinkService = googleCalendarLinkService;
        this.messageSource = messageSource;
    }

    // Shows the travel group overview page for the current user
    @GetMapping
    public String showAllTravelGroups(Model model) {
        model.addAttribute("page", travelGroupService.buildTravelGroupsPage());
        return "travelgroups";
    }

    // Opens the create travel group form and optionally preselects an activity
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

    // Creates a travel group after validating the submitted form values
    @PostMapping("/create")
    public String createTravelGroup(@Valid @ModelAttribute TravelGroupForm form,
                                    BindingResult bindingResult,
                                    Model model) {

        if (bindingResult.hasErrors()) {
            populateCreateTravelGroupModel(model, form);
            return "create-travelgroup";
        }

        try {
            TravelGroup group = travelGroupService.createTravelGroup(
                    form.getActivityId(),
                    form.getMaxMembers(),
                    form.getLocation(),
                    form.getDepartureLocation(),
                    form.getDepartureLatitude(),
                    form.getDepartureLongitude(),
                    form.getMode(),
                    form.getDepartureTime(),
                    form.getEstimatedArrivalTime()
            );

            return "redirect:/activities/" + group.getActivity().getId();
        } catch (ResponseStatusException exception) {
            rejectTravelGroupFormValue(bindingResult, exception);
            populateCreateTravelGroupModel(model, form);
            return "create-travelgroup";
        }
    }

    // Opens the edit form for group owners
    @GetMapping("/{groupId}/edit")
    public String showTravelGroupEditForm(@PathVariable UUID groupId,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);

        if (!travelGroupService.canCurrentUserEditTravelGroup(group)) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute(
                    "toastMessage",
                    message("flash.travelGroup.ownerOnlyEdit")
            );
            return "redirect:/travelgroups/" + groupId;
        }

        TravelGroupForm form = new TravelGroupForm();
        form.setActivityId(group.getActivity().getId());
        form.setMaxMembers(group.getMaxMembers());
        form.setLocation(group.getLocation());
        form.setDepartureLocation(group.getDepartureLocation() != null ? group.getDepartureLocation() : group.getLocation());
        form.setDepartureLatitude(group.getDepartureLatitude());
        form.setDepartureLongitude(group.getDepartureLongitude());
        form.setDepartureTime(group.getDepartureTime());
        form.setEstimatedArrivalTime(group.getEstimatedArrivalTime());
        form.setMode(group.getTransportMode());

        populateEditTravelGroupModel(model, group, form);
        return "edit-travelgroup";
    }

    // Updates an existing travel group owned by the current user
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
            TravelGroup updatedGroup = travelGroupService.updateTravelGroup(
                    groupId,
                    form.getMaxMembers(),
                    form.getLocation(),
                    form.getDepartureLocation(),
                    form.getDepartureLatitude(),
                    form.getDepartureLongitude(),
                    form.getMode(),
                    form.getDepartureTime(),
                    form.getEstimatedArrivalTime()
            );

            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.updated"));
            return "redirect:/travelgroups/" + updatedGroup.getGroupId();
        } catch (ResponseStatusException exception) {
            rejectTravelGroupFormValue(bindingResult, exception);
            populateEditTravelGroupModel(model, group, form);
            return "edit-travelgroup";
        }
    }

    // Joins a group directly or creates a join request depending on approval settings
    @PostMapping("/{groupId}/join")
    public String joinTravelGroup(@PathVariable UUID groupId,
                                  @RequestParam(required = false) String redirectTo,
                                  RedirectAttributes redirectAttributes) {

        try {
            boolean joinApprovalRequired = travelGroupService.isJoinApprovalRequired();
            travelGroupService.joinTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute(
                    "toastMessage",
                    joinApprovalRequired
                            ? message("flash.travelGroup.joinRequestSent")
                            : message("flash.travelGroup.joined")
            );
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        String safeRedirect = safeInternalRedirect(redirectTo);
        if (safeRedirect != null) {
            return safeRedirect;
        }

        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        return "redirect:/travelgroups/" + group.getGroupId();
    }

    // Lets the current member leave a travel group they do not own
    @PostMapping("/{groupId}/leave")
    public String leaveTravelGroup(@PathVariable UUID groupId,
                                   @RequestParam(required = false) String redirectTo,
                                   RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        UUID activityId = group.getActivity().getId();

        try {
            travelGroupService.leaveTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.left"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        String safeRedirect = safeInternalRedirect(redirectTo);
        if (safeRedirect != null) {
            return safeRedirect;
        }

        return "redirect:/activities/" + activityId;
    }

    // Deletes a travel group when the current owner is allowed to remove it
    @PostMapping("/{groupId}/delete")
    public String deleteOwnedTravelGroup(@PathVariable UUID groupId,
                                         @RequestParam(required = false) String redirectTo,
                                         RedirectAttributes redirectAttributes) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        UUID activityId = group.getActivity().getId();

        try {
            travelGroupService.deleteOwnedTravelGroup(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.deleted"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());

            return Objects.requireNonNullElse(safeInternalRedirect(redirectTo), "redirect:/travelgroups/" + groupId);
        }

        return Objects.requireNonNullElse(safeInternalRedirect(redirectTo), "redirect:/activities/" + activityId);
    }

    // Transfers group ownership from the current owner to another joined member
    @PostMapping("/{groupId}/transfer-ownership")
    public String transferOwnership(@PathVariable UUID groupId,
                                    @RequestParam UUID newOwnerId,
                                    @RequestParam(required = false) String redirectTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.transferOwnership(groupId, newOwnerId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.ownershipTransferred"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        String safeRedirect = safeInternalRedirect(redirectTo);
        if (safeRedirect != null) {
            return safeRedirect;
        }

        return "redirect:/travelgroups/" + groupId;
    }

    // Saves the current member's shared pickup/location information for this group
    @PostMapping("/{groupId}/location")
    public String updateSharedLocation(@PathVariable UUID groupId,
                                       @RequestParam String address,
                                       @RequestParam(required = false) Double latitude,
                                       @RequestParam(required = false) Double longitude,
                                       RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.updateCurrentMemberLocation(groupId, address, latitude, longitude);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.locationUpdated"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        return "redirect:/travelgroups/" + groupId;
    }

    // Live updates use JSON so the browser can save new coordinates without refreshing the details page
    @PostMapping("/{groupId}/location/live")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateLiveSharedLocation(@PathVariable UUID groupId,
                                                                        @RequestParam String address,
                                                                        @RequestParam(required = false) Double latitude,
                                                                        @RequestParam(required = false) Double longitude) {
        travelGroupService.updateCurrentMemberLocation(groupId, address, latitude, longitude);

        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "address", address
        ));
    }

    // Removes the current member's shared location without leaving the group
    @PostMapping("/{groupId}/location/clear")
    public String clearSharedLocation(@PathVariable UUID groupId,
                                      RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.clearCurrentMemberLocation(groupId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.locationRemoved"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        return "redirect:/travelgroups/" + groupId;
    }

    // Creates a pending join request for another existing student by email
    @PostMapping("/{groupId}/invite")
    public String inviteMember(@PathVariable UUID groupId,
                               @RequestParam String inviteeEmail,
                               RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.inviteMemberToTravelGroup(groupId, inviteeEmail);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.invitationSent"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
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
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.joinRequestAccepted"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        String safeRedirect = safeInternalRedirect(redirectTo);
        if (safeRedirect != null) {
            return safeRedirect;
        }

        return "redirect:/travelgroups";
    }

    // Rejects a pending join request from the group details page
    @PostMapping("/requests/{requestId}/reject")
    public String rejectJoinRequest(@PathVariable Integer requestId,
                                    @RequestParam(required = false) String redirectTo,
                                    RedirectAttributes redirectAttributes) {
        try {
            travelGroupService.rejectJoinRequest(requestId);
            redirectAttributes.addFlashAttribute("toastType", "success");
            redirectAttributes.addFlashAttribute("toastMessage", message("flash.travelGroup.joinRequestRejected"));
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
        }

        String safeRedirect = safeInternalRedirect(redirectTo);
        if (safeRedirect != null) {
            return safeRedirect;
        }

        return "redirect:/travelgroups";
    }

    // Shows all details for one travel group including members requests and location sharing
    @GetMapping("/{groupId}")
    public String showTravelGroupDetails(@PathVariable UUID groupId, Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        List<TravelGroupMember> groupMembers = travelGroupService.getMembersForGroup(group);
        boolean joined = travelGroupService.isCurrentUserMember(group);
        boolean owner = travelGroupService.isCurrentUserOwner(group);
        JoinRequestStatus joinRequestStatus = travelGroupService.getCurrentUserJoinRequestStatus(group);
        boolean joinApprovalRequired = travelGroupService.isJoinApprovalRequired();
        long memberCount = groupMembers.size();

        model.addAttribute("group", group);
        model.addAttribute("groupMembers", groupMembers);
        model.addAttribute("currentUserMembership", travelGroupService.getCurrentUserMembership(group));
        // Makes the small calendar icon ready only when the group has a departure time
        model.addAttribute("googleCalendarUrl", googleCalendarLinkService.buildTravelGroupCalendarUrl(group));
        model.addAttribute("activityLogCount", travelGroupService.countActivityLogEntries(group));
        model.addAttribute("pendingJoinRequestCount", travelGroupService.getVisiblePendingRequests(group).size());
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

    // Shows travel group activity log
    @GetMapping("/{groupId}/activity-log")
    public String showTravelGroupActivityLog(@PathVariable UUID groupId,
                                             @RequestParam(defaultValue = "0") int page,
                                             Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        Page<TravelGroupActivityLogView> activityLogPage = travelGroupService.getActivityLogPage(group, page, 10);

        model.addAttribute("group", group);
        model.addAttribute("activityLogPage", activityLogPage);
        model.addAttribute("activityLogEntries", activityLogPage.getContent());
        model.addAttribute("isOwner", travelGroupService.isCurrentUserOwner(group));
        model.addAttribute("joinApprovalRequired", travelGroupService.isJoinApprovalRequired());
        model.addAttribute("pendingJoinRequestCount", travelGroupService.getVisiblePendingRequests(group).size());

        return "travelgroup-activity-log";
    }

    // Shows the De Lijn route suggestion page for a public transport travel group
    @GetMapping("/{groupId}/route-suggestions")
    public String showRouteSuggestions(@PathVariable UUID groupId, Model model) {
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);

        model.addAttribute("group", group);
        model.addAttribute("routeSuggestionsUrl", "/api/travelgroups/" + group.getGroupId() + "/route-suggestions?maxResults=8");

        return "travelgroup-route-suggestions";
    }

    // Adds shared model data needed by the create travel group form
    private void populateCreateTravelGroupModel(Model model, TravelGroupForm form) {
        List<Activity> activities = activityService.getAllActivities();
        model.addAttribute("travelGroupForm", form);
        model.addAttribute("activities", activities);
        model.addAttribute(
                "activityDepartureLimits",
                activities.stream().collect(Collectors.toMap(Activity::getId, this::formatActivityDateTime))
        );
        model.addAttribute("minDepartureTime", formatDateTime(LocalDateTime.now()));

        if (form.getActivityId() != null) {
            Activity selectedActivity = activityService.getActivityById(form.getActivityId());
            model.addAttribute("selectedActivity", selectedActivity);
            model.addAttribute("maxDepartureTime", formatActivityDateTime(selectedActivity));
        }
    }

    // Adds shared model data needed by the edit travel group form
    private void populateEditTravelGroupModel(Model model, TravelGroup group, TravelGroupForm form) {
        model.addAttribute("group", group);
        model.addAttribute("travelGroupForm", form);
        model.addAttribute("memberCount", travelGroupService.getMemberCount(group));
        model.addAttribute("ownershipTransferCandidates", travelGroupService.getOwnershipTransferCandidates(group));
        model.addAttribute("minDepartureTime", formatDateTime(LocalDateTime.now()));
        model.addAttribute("maxDepartureTime", formatActivityDateTime(group.getActivity()));
    }

    // Maps service validation errors back to the most relevant form field
    private void rejectTravelGroupFormValue(BindingResult bindingResult, ResponseStatusException exception) {
        String reason = exception.getReason();
        String field = "departureTime";

        if (reason != null && (reason.startsWith("Maximum members") || reason.startsWith("Max members"))) {
            field = "maxMembers";
        } else if (reason != null && (reason.startsWith("Departure location")
                || reason.startsWith("Departure latitude")
                || reason.startsWith("Departure longitude"))) {
            field = "departureLocation";
        } else if (reason != null && reason.startsWith("Estimated arrival")) {
            field = "estimatedArrivalTime";
        } else if (reason != null && reason.startsWith("Transport")) {
            field = "mode";
        } else if (reason != null && reason.startsWith("Activity")) {
            field = "activityId";
        }

        bindingResult.rejectValue(
                field,
                "travelGroupForm." + field + ".invalid",
                exception.getReason() != null ? exception.getReason() : "Invalid value"
        );
    }

    // Formats an activity date and time as an HTML datetime-local maximum value
    private String formatActivityDateTime(Activity activity) {
        if (activity.getDate() == null || activity.getTime() == null) {
            return "";
        }

        return formatDateTime(LocalDateTime.of(activity.getDate(), activity.getTime()));
    }

    // Formats a date and time for HTML datetime-local inputs
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_INPUT_FORMATTER);
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    // Allows redirect targets only when they stay inside this application
    private static String safeInternalRedirect(String redirectTo) {
        if (redirectTo == null || redirectTo.isBlank()) {
            return null;
        }

        String target = redirectTo.trim();
        if (!target.startsWith("/") || target.startsWith("//") || target.contains("\r") || target.contains("\n")) {
            return null;
        }

        return "redirect:" + target;
    }
}