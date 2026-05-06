package TrackTogether.controller;

import TrackTogether.domain.Activity;
import TrackTogether.domain.Member;
import TrackTogether.service.ActivityService;
import TrackTogether.service.MemberService;
import TrackTogether.service.TravelGroupService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final MemberService memberService;
    private final TravelGroupService travelGroupService;

    public ActivityController(ActivityService activityService,
                              MemberService memberService,
                              TravelGroupService travelGroupService) {
        this.activityService = activityService;
        this.memberService = memberService;
        this.travelGroupService = travelGroupService;
    }

    @GetMapping
    public String getAllActivities(Model model) {
        List<Activity> activities = activityService.getAllActivities();
        model.addAttribute("activities",
                activities != null ? activities : List.of());
        return "activities";
    }

    @GetMapping("/new")
    public String showNewActivityPage(Model model) {
        Member member = (Member) model.getAttribute("currentUser");
        model.addAttribute("user", member);
        return "newActivity";
    }

    @PostMapping("/new")
    public String createActivity(Model model,
                                 @RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String location,
                                 @RequestParam String date,
                                 @RequestParam String time,
                                 @RequestParam(required = false) Double latitude,
                                 @RequestParam(required = false) Double longitude,
                                 Principal principal) {

        Member member = (Member) model.getAttribute("currentUser");

        LocalDate activityDate = LocalDate.parse(date);
        LocalTime activityTime = LocalTime.parse(time);

        LocalDateTime activityDateTime = LocalDateTime.of(activityDate, activityTime);

        if (activityDateTime.isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "You cannot create an activity in the past.");
            return "newActivity";
        }

        Activity activity = new Activity();
        activity.setCreator(member);
        activity.setName(name);
        activity.setDescription(description);
        activity.setLocation(location);
        activity.setDate(activityDate);
        activity.setTime(activityTime);

        // CRUCIAL
        activity.setLatitude(latitude);
        activity.setLongitude(longitude);

        activityService.createActivity(principal.getName(), activity);

        return "redirect:/activities";
    }

    @GetMapping("/{id}")
    public String getActivityById(@PathVariable UUID id, Model model) {
        Activity activity = activityService.getActivityById(id);
        var activityTravelGroups = travelGroupService.getTravelGroupsForActivity(id);

        model.addAttribute("activity", activity);
        model.addAttribute("activityTravelGroups", activityTravelGroups);
        model.addAttribute("joinedGroupIds", travelGroupService.getJoinedGroupIds(activityTravelGroups));
        model.addAttribute("ownedGroupIds", travelGroupService.getOwnedGroupIds(activityTravelGroups));
        model.addAttribute("memberCounts", travelGroupService.getMemberCounts(activityTravelGroups));
        model.addAttribute("pendingJoinRequestGroupIds", travelGroupService.getPendingJoinRequestGroupIds(activityTravelGroups));
        model.addAttribute("rejectedJoinRequestGroupIds", travelGroupService.getRejectedJoinRequestGroupIds(activityTravelGroups));
        model.addAttribute("pendingJoinRequestCounts", travelGroupService.getPendingJoinRequestCounts(activityTravelGroups));
        return "activity-overview";
    }

    @PostMapping("/{id}/delete")
    public String deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);
        return "redirect:/activities";
    }
}