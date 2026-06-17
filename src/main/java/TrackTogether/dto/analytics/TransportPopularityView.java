package TrackTogether.dto.analytics;

public record TransportPopularityView(
        String mostPopularTransportMode,
        long mostPopularTransportCount,
        String leastPopularTransportMode,
        long leastPopularTransportCount
) {
}