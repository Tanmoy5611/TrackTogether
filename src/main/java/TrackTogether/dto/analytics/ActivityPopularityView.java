package TrackTogether.dto.analytics;

public record ActivityPopularityView(
        String activityName,
        long participantCount
) {
}