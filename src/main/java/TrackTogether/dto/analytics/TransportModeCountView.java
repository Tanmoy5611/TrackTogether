package TrackTogether.dto.analytics;

public record TransportModeCountView(
        String transportMode,
        long memberCount
) {
}