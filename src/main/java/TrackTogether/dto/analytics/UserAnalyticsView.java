package TrackTogether.dto.analytics;

import java.util.List;

public record UserAnalyticsView(
        long totalTripsCreated,
        long totalJoinedTrips,
        double personalBaselineEmissionsKg,
        double personalActualEmissionsKg,
        double personalSavingsPercentage,
        List<TransportModeCountView> transportModeBreakdown,
        double personalCo2SavingsKg
) {
}