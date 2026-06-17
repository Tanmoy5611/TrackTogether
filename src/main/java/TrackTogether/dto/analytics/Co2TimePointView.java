package TrackTogether.dto.analytics;

public record Co2TimePointView(
        String period,
        double baselineEmissionsKg,
        double actualEmissionsKg,
        double savingsKg,
        long eventCount,
        long groupedUserCount
) {
}