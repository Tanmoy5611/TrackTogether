package TrackTogether.dto.analytics;

import java.util.List;

public record AdminAnalyticsView(
        long totalEvents,
        long totalTravelGroups,
        long totalParticipants,
        double averageGroupSize,
        List<ActivityPopularityView> mostPopularActivities,
        TransportPopularityView transportPopularity,
        List<TransportModeCountView> transportModeBreakdown,
        List<PeakTravelTimeView> peakTravelTimes,
        double totalBaselineEmissionsKg,
        double totalActualEmissionsKg,
        double savingsPercentage,
        double totalSavingsKg,
        List<ActivityCo2SavingsView> co2SavingsPerActivity,
        List<TripCo2SavingsView> co2SavingsPerTrip
) {
}