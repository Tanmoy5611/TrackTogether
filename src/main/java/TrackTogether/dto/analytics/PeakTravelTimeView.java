package TrackTogether.dto.analytics;

public record PeakTravelTimeView(
        String timeBucket,
        long travelGroupCount
) {
}