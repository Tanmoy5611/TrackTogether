package TrackTogether.dto.analytics;

public record TripCo2SavingsView(
        String activityName,
        String transportMode,
        long memberCount,
        double baselineEmissionsKg,
        double actualEmissionsKg,
        double savingsKg
) {
}