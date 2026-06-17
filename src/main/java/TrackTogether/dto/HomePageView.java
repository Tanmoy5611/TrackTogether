package TrackTogether.dto;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TravelGroup;
import lombok.Getter;

import java.util.List;

@Getter
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
    private final List<TravelFriendSuggestionDto> suggestedTravelGroups;
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
                        List<TravelFriendSuggestionDto> suggestedTravelGroups,
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
        this.suggestedTravelGroups = suggestedTravelGroups;
        this.co2Saved = co2Saved;
    }
}