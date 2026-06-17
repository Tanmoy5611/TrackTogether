package TrackTogether.dto.analytics;

public record ActivityCo2SavingsView(
        String activityName,
        double baselineEmissionsKg,
        double actualEmissionsKg,
        double savingsKg
) {
}