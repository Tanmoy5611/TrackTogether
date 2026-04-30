package TrackTogether.service;

import TrackTogether.dto.HomePageView;
import TrackTogether.dto.HomeSuggestionReason;
import TrackTogether.dto.HomeSuggestionView;
import TrackTogether.domain.Activity;
import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HomePageService {

    // Services reused to gather activity, travel group, and current user data
    private final ActivityService activityService;
    private final TravelGroupService travelGroupService;
    private final CurrentUserService currentUserService;

    public HomePageService(ActivityService activityService,
                           TravelGroupService travelGroupService,
                           CurrentUserService currentUserService) {
        this.activityService = activityService;
        this.travelGroupService = travelGroupService;
        this.currentUserService = currentUserService;
    }

    public HomePageView buildHomePage() {
        // Get the logged-in member first because the homepage is personalized
        Member currentUser = currentUserService.getCurrentUser();

        // Collect all activities and ignore any unexpected null values
        List<Activity> allActivities = activityService.getAllActivities()
                .stream()
                .filter(Objects::nonNull)
                .toList();

        // Only keep activities that are upcoming and sort them chronologically
        List<Activity> upcomingActivities = allActivities.stream()
                .filter(activity -> activity.getDate() != null)
                .filter(activity -> !activity.getDate().isBefore(LocalDate.now()))
                .sorted(activityComparator())
                .toList();

        // Separate activities created by the current user from the wider community list
        List<Activity> myUpcomingActivities = upcomingActivities.stream()
                .filter(activity -> isOwnedByCurrentUser(activity, currentUser))
                .limit(4)
                .toList();

        List<Activity> communityUpcomingActivities = upcomingActivities.stream()
                .filter(activity -> !isOwnedByCurrentUser(activity, currentUser))
                .toList();

        // Build small matching sets so we can suggest activities by location and time
        Set<String> myLocations = myUpcomingActivities.stream()
                .map(Activity::getLocation)
                .map(HomePageService::normalize)
                .filter(location -> !location.isBlank())
                .collect(Collectors.toSet());

        Set<String> myTimeKeys = myUpcomingActivities.stream()
                .map(HomePageService::timeKey)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toSet());

        // Find the best matching suggestions from the community activities
        List<HomeSuggestionView> matchedSuggestions = communityUpcomingActivities.stream()
                .map(activity -> toSuggestion(activity, myLocations, myTimeKeys))
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        // If there are no strong matches yet, fall back to simple upcoming suggestions
        List<HomeSuggestionView> suggestedActivities = matchedSuggestions.isEmpty()
                ? communityUpcomingActivities.stream()
                .limit(3)
                .map(activity -> new HomeSuggestionView(activity, HomeSuggestionReason.UPCOMING_THIS_WEEK))
                .toList()
                : matchedSuggestions;

        long locationSuggestionCount = matchedSuggestions.stream()
                .filter(suggestion -> suggestion.getReason() == HomeSuggestionReason.SAME_LOCATION)
                .count();

        long timeSuggestionCount = matchedSuggestions.stream()
                .filter(suggestion -> suggestion.getReason() == HomeSuggestionReason.SAME_TIME)
                .count();

        // Keep the homepage travel group block lightweight
        List<TravelGroup> openTravelGroups = travelGroupService.getAllTravelGroups()
                .stream()
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        // The latest activity card prefers the user's own upcoming activity first
        Activity latestActivity = !myUpcomingActivities.isEmpty()
                ? myUpcomingActivities.getFirst()
                : (!communityUpcomingActivities.isEmpty() ? communityUpcomingActivities.getFirst() : null);

        // Decide which activities should actually be shown in the "upcoming" panel
        List<Activity> displayedUpcomingActivities = !myUpcomingActivities.isEmpty()
                ? myUpcomingActivities
                : communityUpcomingActivities.stream().limit(4).toList();

        // Package everything into one DTO that the controller can send to the view
        return new HomePageView(
                buildWelcomeTitle(currentUser),
                buildWelcomeSubtitle(myUpcomingActivities, openTravelGroups),
                buildLatestHeadline(latestActivity, myUpcomingActivities.isEmpty()),
                buildLatestMeta(latestActivity),
                "/activities",
                latestActivity != null ? "Open event" : "Browse activities",
                displayedUpcomingActivities,
                suggestedActivities,
                locationSuggestionCount,
                timeSuggestionCount,
                openTravelGroups,
                currentUser.getCo2Saved()
        );
    }

    private static Comparator<Activity> activityComparator() {
        return Comparator.comparing(Activity::getDate)
                .thenComparing(Activity::getTime, Comparator.nullsLast(LocalTime::compareTo))
                .thenComparing(activity -> normalize(activity.getName()));
    }

    // Check whether the activity belongs to the logged-in member
    private static boolean isOwnedByCurrentUser(Activity activity, Member currentUser) {
        return activity.getCreator() != null
                && activity.getCreator().getUserId() != null
                && activity.getCreator().getUserId().equals(currentUser.getUserId());
    }

    // Normalize text values before comparing them
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // Combine date and time into one comparable key
    private static String timeKey(Activity activity) {
        if (activity.getDate() == null || activity.getTime() == null) {
            return "";
        }

        return activity.getDate() + "|" + activity.getTime();
    }

    private static HomeSuggestionView toSuggestion(Activity activity,
                                                   Set<String> myLocations,
                                                   Set<String> myTimeKeys) {
        // Prefer location match first, otherwise try time match
        if (myLocations.contains(normalize(activity.getLocation()))) {
            return new HomeSuggestionView(activity, HomeSuggestionReason.SAME_LOCATION);
        }

        if (myTimeKeys.contains(timeKey(activity))) {
            return new HomeSuggestionView(activity, HomeSuggestionReason.SAME_TIME);
        }

        return null;
    }

    private static String buildWelcomeTitle(Member currentUser) {
        String name = currentUser.getName();

        if (name == null || name.isBlank()) {
            return "Good to see you again.";
        }

        return "Welcome back, " + name + ".";
    }

    private static String buildWelcomeSubtitle(List<Activity> myUpcomingActivities,
                                               List<TravelGroup> openTravelGroups) {
        // Adjust the subtitle depending on whether the user already has upcoming plans
        if (!myUpcomingActivities.isEmpty()) {
            return "You already have plans ahead. Here is a clean view of what is next, plus matching suggestions around your schedule.";
        }

        if (!openTravelGroups.isEmpty()) {
            return "No personal events yet, but there are travel groups and upcoming activities you can jump into right away.";
        }

        return "Start with upcoming activities, then use suggestions and travel groups to make coordination easier.";
    }

    private static String buildLatestHeadline(Activity latestActivity, boolean communityFallback) {
        if (latestActivity == null) {
            return "Your home page is ready for the next activity.";
        }

        if (communityFallback) {
            return latestActivity.getName() + " is one of the next activities you can explore.";
        }

        return latestActivity.getName() + " is the next event on your radar.";
    }

    private static String buildLatestMeta(Activity latestActivity) {
        // Build the short line that appears under the latest update card
        if (latestActivity == null) {
            return "As soon as new events or travel opportunities are available, they will show up here.";
        }

        String location = latestActivity.getLocation() == null || latestActivity.getLocation().isBlank()
                ? "Location to be confirmed"
                : latestActivity.getLocation();

        return location + " · " + latestActivity.getDate() + " · " + latestActivity.getTime();
    }
}
