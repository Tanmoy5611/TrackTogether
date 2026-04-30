package TrackTogether.dto;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TravelGroup;

import java.util.List;

// DTO used to send all homepage data from the service layer to the Thymeleaf view
public class HomePageView {

    private final String welcomeTitle;
    private final String welcomeSubtitle;
    private final String latestHeadline;
    private final String latestMeta;
    private final String latestLink;
    private final String latestLinkLabel;
    private final List<Activity> upcomingActivities;
    private final List<HomeSuggestionView> suggestedActivities;
    private final long locationSuggestionCount;
    private final long timeSuggestionCount;
    private final List<TravelGroup> openTravelGroups;
    private final double co2Saved;

    public HomePageView(String welcomeTitle,
                        String welcomeSubtitle,
                        String latestHeadline,
                        String latestMeta,
                        String latestLink,
                        String latestLinkLabel,
                        List<Activity> upcomingActivities,
                        List<HomeSuggestionView> suggestedActivities,
                        long locationSuggestionCount,
                        long timeSuggestionCount,
                        List<TravelGroup> openTravelGroups,
                        double co2Saved) {
        this.welcomeTitle = welcomeTitle;
        this.welcomeSubtitle = welcomeSubtitle;
        this.latestHeadline = latestHeadline;
        this.latestMeta = latestMeta;
        this.latestLink = latestLink;
        this.latestLinkLabel = latestLinkLabel;
        this.upcomingActivities = upcomingActivities;
        this.suggestedActivities = suggestedActivities;
        this.locationSuggestionCount = locationSuggestionCount;
        this.timeSuggestionCount = timeSuggestionCount;
        this.openTravelGroups = openTravelGroups;
        this.co2Saved = co2Saved;
    }

    // Basic text shown in the hero section
    public String getWelcomeTitle() {
        return welcomeTitle;
    }

    public String getWelcomeSubtitle() {
        return welcomeSubtitle;
    }

    public String getLatestHeadline() {
        return latestHeadline;
    }

    public String getLatestMeta() {
        return latestMeta;
    }

    public String getLatestLink() {
        return latestLink;
    }

    public String getLatestLinkLabel() {
        return latestLinkLabel;
    }

    // Lists used by the different homepage sections
    public List<Activity> getUpcomingActivities() {
        return upcomingActivities;
    }

    public List<HomeSuggestionView> getSuggestedActivities() {
        return suggestedActivities;
    }

    public long getLocationSuggestionCount() {
        return locationSuggestionCount;
    }

    public long getTimeSuggestionCount() {
        return timeSuggestionCount;
    }

    public List<TravelGroup> getOpenTravelGroups() {
        return openTravelGroups;
    }

    // Simple personal metric shown in the stats area
    public double getCo2Saved() {
        return co2Saved;
    }
}
