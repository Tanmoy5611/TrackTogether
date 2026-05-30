package TrackTogether.service;

import TrackTogether.dto.HomePageView;
import TrackTogether.dto.HomeSuggestionReason;
import TrackTogether.dto.HomeSuggestionView;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.dto.analytics.UserAnalyticsView;
import TrackTogether.domain.Activity;
import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HomePageService {

    // Services reused to gather activity, travel group, and current user data
    private final ActivityService activityService;
    private final TravelGroupService travelGroupService;
    private final CurrentUserService currentUserService;
    private final FriendMatchingService friendMatchingService;
    private final AnalyticsService analyticsService;
    private final MessageSource messageSource;

    public HomePageService(ActivityService activityService,
                           TravelGroupService travelGroupService,
                           CurrentUserService currentUserService,
                           FriendMatchingService friendMatchingService,
                           AnalyticsService analyticsService,
                           MessageSource messageSource) {
        this.activityService = activityService;
        this.travelGroupService = travelGroupService;
        this.currentUserService = currentUserService;
        this.friendMatchingService = friendMatchingService;
        this.analyticsService = analyticsService;
        this.messageSource = messageSource;
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

        List<LocalTime> myActivityTimes = myUpcomingActivities.stream()
                .map(Activity::getTime)
                .filter(Objects::nonNull)
                .toList();

        // Find the best matching suggestions from the community activities
        List<HomeSuggestionView> matchedSuggestions = communityUpcomingActivities.stream()
                .map(activity -> toSuggestion(activity, myLocations, myActivityTimes))
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

        // Reuse batch data here so the home dashboard stays quick
        List<TravelGroup> allTravelGroups = travelGroupService.getAllTravelGroups();
        Set<UUID> joinedTravelGroupIds = travelGroupService.getJoinedGroupIds(allTravelGroups);
        Set<UUID> ownedTravelGroupIds = travelGroupService.getOwnedGroupIds(allTravelGroups);
        Map<UUID, Long> travelGroupMemberCounts = travelGroupService.getMemberCounts(allTravelGroups);

        // Only count groups the user can actually still join from the home page
        List<TravelGroup> openTravelGroups = allTravelGroups
                .stream()
                .filter(Objects::nonNull)
                .filter(HomePageService::isUpcomingOrUndated)
                .filter(group -> !joinedTravelGroupIds.contains(group.getGroupId()))
                .filter(group -> !ownedTravelGroupIds.contains(group.getGroupId()))
                .filter(group -> group.getMaxMembers() != null)
                .filter(group -> group.hasAvailableSpots(travelGroupMemberCounts.getOrDefault(group.getGroupId(), 0L)))
                .toList();

        List<TravelFriendSuggestionDto> suggestedTravelGroups = friendMatchingService
                .suggestTravelGroupsForCurrentUser()
                .stream()
                .limit(3)
                .toList();

        UserAnalyticsView userAnalytics = analyticsService.getUserAnalytics(currentUser);

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
                latestActivity != null ? "/activities/" + latestActivity.getId() : "/activities",
                latestActivity != null ? message("home.latest.link.openEvent") : message("home.latest.link.browseActivities"),
                displayedUpcomingActivities,
                suggestedActivities,
                locationSuggestionCount,
                timeSuggestionCount,
                openTravelGroups,
                suggestedTravelGroups,
                userAnalytics.personalCo2SavingsKg()
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

    private static boolean isUpcomingOrUndated(TravelGroup group) {
        // Past groups should not make the dashboard look active
        if (group.getDepartureTime() != null) {
            return !group.getDepartureTime().isBefore(LocalDateTime.now());
        }

        Activity activity = group.getActivity();
        if (activity == null || activity.getDate() == null) {
            return true;
        }

        if (activity.getTime() == null) {
            return !activity.getDate().isBefore(LocalDate.now());
        }

        return !LocalDateTime.of(activity.getDate(), activity.getTime()).isBefore(LocalDateTime.now());
    }

    // Normalize text values before comparing them
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static HomeSuggestionView toSuggestion(Activity activity,
                                                   Set<String> myLocations,
                                                   List<LocalTime> myActivityTimes) {
        // Prefer location match first, otherwise try time match
        if (myLocations.contains(normalize(activity.getLocation()))) {
            return new HomeSuggestionView(activity, HomeSuggestionReason.SAME_LOCATION);
        }

        if (happensAroundKnownTime(activity, myActivityTimes)) {
            return new HomeSuggestionView(activity, HomeSuggestionReason.SAME_TIME);
        }

        return null;
    }

    private static boolean happensAroundKnownTime(Activity activity, List<LocalTime> knownTimes) {
        if (activity.getTime() == null || knownTimes.isEmpty()) {
            return false;
        }

        return knownTimes.stream()
                .anyMatch(knownTime -> Math.abs(Duration.between(knownTime, activity.getTime()).toMinutes()) <= 60);
    }

    private String buildWelcomeTitle(Member currentUser) {
        String name = currentUser.getName();

        if (name == null || name.isBlank()) {
            return message("home.welcome.noName");
        }

        return message("home.welcome.withName", name);
    }

    private String buildWelcomeSubtitle(List<Activity> myUpcomingActivities,
                                        List<TravelGroup> openTravelGroups) {
        // Adjust the subtitle depending on whether the user already has upcoming plans
        if (!myUpcomingActivities.isEmpty()) {
            return message("home.subtitle.hasPlans");
        }

        if (!openTravelGroups.isEmpty()) {
            return message("home.subtitle.hasGroups");
        }

        return message("home.subtitle.empty");
    }

    private String buildLatestHeadline(Activity latestActivity, boolean communityFallback) {
        if (latestActivity == null) {
            return message("home.latest.noActivity");
        }

        if (communityFallback) {
            return message("home.latest.community", latestActivity.getName());
        }

        return message("home.latest.personal", latestActivity.getName());
    }

    private String buildLatestMeta(Activity latestActivity) {
        // Build the short line that appears under the latest update card
        if (latestActivity == null) {
            return message("home.latest.meta.empty");
        }

        String location = latestActivity.getLocation() == null || latestActivity.getLocation().isBlank()
                ? message("home.latest.meta.locationUnknown")
                : latestActivity.getLocation();

        return location + " - " + latestActivity.getDate() + " - " + latestActivity.getTime();
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}