package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.dto.analytics.ActivityCo2SavingsView;
import TrackTogether.dto.analytics.ActivityPopularityView;
import TrackTogether.dto.analytics.AdminAnalyticsView;
import TrackTogether.dto.analytics.Co2Period;
import TrackTogether.dto.analytics.Co2TimePointView;
import TrackTogether.dto.analytics.PeakTravelTimeView;
import TrackTogether.dto.analytics.TransportModeCountView;
import TrackTogether.dto.analytics.TransportPopularityView;
import TrackTogether.dto.analytics.TripCo2SavingsView;
import TrackTogether.dto.analytics.UserAnalyticsView;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsService {

    public static final double DEFAULT_DISTANCE_KM = 10.0;
    public static final double CAR_KG_CO2_PER_KM = 0.120;
    public static final double PUBLIC_TRANSPORT_KG_CO2_PER_KM = 0.040;
    public static final double ZERO_EMISSIONS_KG_CO2_PER_KM = 0.0;

    private final ActivityRepository activityRepository;
    private final TravelGroupRepository travelGroupRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final MessageSource messageSource;

    public AnalyticsService(ActivityRepository activityRepository,
                            TravelGroupRepository travelGroupRepository,
                            TravelGroupMemberRepository travelGroupMemberRepository,
                            MessageSource messageSource) {
        this.activityRepository = activityRepository;
        this.travelGroupRepository = travelGroupRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.messageSource = messageSource;
    }

    public UserAnalyticsView getUserAnalytics(Member member) {
        long createdTrips = activityRepository.countByCreator(member);
        List<TravelGroupMember> memberships = travelGroupMemberRepository.findAllByMember(member);
        long joinedTrips = memberships.size();
        // Preload member counts because the CO2 formulas need group size more than once
        Map<UUID, Long> memberCounts = memberCountsByGroupId(memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .toList());

        double personalBaseline = memberships.stream()
                .mapToDouble(this::calculateBaselineForMembership)
                .sum();

        double personalActual = memberships.stream()
                .mapToDouble(membership -> calculateActualForMembership(membership, memberCounts))
                .sum();

        double personalSavings = personalBaseline - personalActual;

        return new UserAnalyticsView(
                createdTrips,
                joinedTrips,
                round(personalBaseline),
                round(personalActual),
                percentage(personalSavings, personalBaseline),
                getTransportModeBreakdown(memberships),
                round(personalSavings)
        );
    }

    public AdminAnalyticsView getAdminAnalytics() {
        List<TravelGroup> groups = travelGroupRepository.findAll();
        List<TravelGroupMember> memberships = travelGroupMemberRepository.findAll();
        // Use one grouped count query for all admin CO2 calculations
        Map<UUID, Long> memberCounts = memberCountsByGroupId(groups);

        double averageGroupSize = groups.isEmpty()
                ? 0
                : (double) memberships.size() / groups.size();

        long totalEvents = groups.stream()
                .map(TravelGroup::getActivity)
                .filter(Objects::nonNull)
                .map(Activity::getId)
                .distinct()
                .count();

        List<ActivityPopularityView> popularActivities = memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getActivity)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Activity::getName, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new ActivityPopularityView(entry.getKey(), entry.getValue()))
                .toList();

        TransportPopularityView transportPopularity = getTransportPopularity(memberships);
        List<TransportModeCountView> transportModeBreakdown = getTransportModeBreakdown(memberships);
        List<PeakTravelTimeView> peakTimes = getPeakTravelTimes(groups);

        double totalBaseline = groups.stream()
                .mapToDouble(group -> calculateBaselineForGroup(group, memberCounts))
                .sum();

        double totalActual = groups.stream()
                .mapToDouble(group -> calculateActualForGroup(group, memberCounts))
                .sum();

        List<ActivityCo2SavingsView> co2SavingsPerActivity = getCo2SavingsPerActivity(groups, memberCounts);
        List<TripCo2SavingsView> co2SavingsPerTrip = getCo2SavingsPerTrip(groups, memberCounts);

        return new AdminAnalyticsView(
                totalEvents,
                groups.size(),
                memberships.size(),
                round(averageGroupSize),
                popularActivities,
                transportPopularity,
                transportModeBreakdown,
                peakTimes,
                round(totalBaseline),
                round(totalActual),
                percentage(totalBaseline - totalActual, totalBaseline),
                round(totalBaseline - totalActual),
                co2SavingsPerActivity,
                co2SavingsPerTrip
        );
    }

    public List<Co2TimePointView> getCo2ThroughTime(Co2Period period) {
        Map<String, List<TravelGroup>> groupsByPeriod = travelGroupRepository.findAll()
                .stream()
                .filter(group -> group.getActivity() != null && group.getActivity().getDate() != null)
                .collect(Collectors.groupingBy(group -> formatPeriod(group.getActivity().getDate(), period)));
        List<TravelGroup> allGroups = groupsByPeriod.values()
                .stream()
                .flatMap(List::stream)
                .toList();
        // Load counts once before calculating every time bucket
        Map<UUID, Long> memberCounts = memberCountsByGroupId(allGroups);

        return groupsByPeriod.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<TravelGroup> groups = entry.getValue();

                    double baseline = groups.stream()
                            .mapToDouble(group -> calculateBaselineForGroup(group, memberCounts))
                            .sum();

                    double actual = groups.stream()
                            .mapToDouble(group -> calculateActualForGroup(group, memberCounts))
                            .sum();

                    long events = groups.stream()
                            .map(TravelGroup::getActivity)
                            .filter(Objects::nonNull)
                            .map(Activity::getId)
                            .distinct()
                            .count();

                    long groupedUsers = groups.stream()
                            .mapToLong(group -> memberCount(group, memberCounts))
                            .sum();

                    return new Co2TimePointView(
                            entry.getKey(),
                            round(baseline),
                            round(actual),
                            round(baseline - actual),
                            events,
                            groupedUsers
                    );
                })
                .toList();
    }

    private TransportPopularityView getTransportPopularity(List<TravelGroupMember> memberships) {
        Map<TransportMode, Long> counts = countTransportModes(memberships);

        Map.Entry<TransportMode, Long> most = counts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry(TransportMode.CAR, 0L));

        Map.Entry<TransportMode, Long> least = counts.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .orElse(Map.entry(TransportMode.CAR, 0L));

        return new TransportPopularityView(
                displayMode(most.getKey()),
                most.getValue(),
                displayMode(least.getKey()),
                least.getValue()
        );
    }

    private List<TransportModeCountView> getTransportModeBreakdown(List<TravelGroupMember> memberships) {
        Map<TransportMode, Long> counts = countTransportModes(memberships);

        return counts.entrySet()
                .stream()
                .map(entry -> new TransportModeCountView(displayMode(entry.getKey()), entry.getValue()))
                .toList();
    }

    private Map<TransportMode, Long> countTransportModes(List<TravelGroupMember> memberships) {
        Map<TransportMode, Long> counts = new EnumMap<>(TransportMode.class);

        for (TransportMode mode : TransportMode.values()) {
            counts.put(mode, 0L);
        }

        memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getTransportMode)
                .filter(Objects::nonNull)
                .forEach(mode -> counts.put(mode, counts.get(mode) + 1));

        return counts;
    }

    private List<PeakTravelTimeView> getPeakTravelTimes(List<TravelGroup> groups) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(message("analytics.time.morning"), 0L);
        counts.put(message("analytics.time.daytime"), 0L);
        counts.put(message("analytics.time.evening"), 0L);
        counts.put(message("analytics.time.night"), 0L);

        groups.stream()
                .map(TravelGroup::getActivity)
                .filter(Objects::nonNull)
                .map(Activity::getTime)
                .filter(Objects::nonNull)
                .map(this::timeBucket)
                .forEach(bucket -> counts.put(bucket, counts.get(bucket) + 1));

        return counts.entrySet()
                .stream()
                .map(entry -> new PeakTravelTimeView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ActivityCo2SavingsView> getCo2SavingsPerActivity(List<TravelGroup> groups,
                                                                  Map<UUID, Long> memberCounts) {
        return groups.stream()
                .filter(group -> group.getActivity() != null)
                .collect(Collectors.groupingBy(group -> group.getActivity().getName()))
                .entrySet()
                .stream()
                .map(entry -> {
                    double baseline = entry.getValue()
                            .stream()
                            .mapToDouble(group -> calculateBaselineForGroup(group, memberCounts))
                            .sum();

                    double actual = entry.getValue()
                            .stream()
                            .mapToDouble(group -> calculateActualForGroup(group, memberCounts))
                            .sum();

                    return new ActivityCo2SavingsView(
                            entry.getKey(),
                            round(baseline),
                            round(actual),
                            round(baseline - actual)
                    );
                })
                .sorted(Comparator.comparing(ActivityCo2SavingsView::savingsKg).reversed())
                .toList();
    }

    private List<TripCo2SavingsView> getCo2SavingsPerTrip(List<TravelGroup> groups,
                                                          Map<UUID, Long> memberCounts) {
        return groups.stream()
                .filter(group -> group.getActivity() != null)
                .map(group -> {
                    double baseline = calculateBaselineForGroup(group, memberCounts);
                    double actual = calculateActualForGroup(group, memberCounts);

                    return new TripCo2SavingsView(
                            group.getActivity().getName(),
                            displayMode(group.getTransportMode()),
                            memberCount(group, memberCounts),
                            round(baseline),
                            round(actual),
                            round(baseline - actual)
                    );
                })
                .sorted(Comparator.comparing(TripCo2SavingsView::savingsKg).reversed())
                .limit(10)
                .toList();
    }

    private double calculateBaselineForMembership(TravelGroupMember membership) {
        TravelGroup group = membership.getGroup();

        if (group == null) {
            return 0;
        }

        double distance = distanceKm(group);

        return distance * CAR_KG_CO2_PER_KM;
    }

    private double calculateActualForMembership(TravelGroupMember membership) {
        return calculateActualForMembership(membership, Map.of());
    }

    private double calculateActualForMembership(TravelGroupMember membership, Map<UUID, Long> memberCounts) {
        TravelGroup group = membership.getGroup();

        if (group == null || group.getTransportMode() == null) {
            return 0;
        }

        double distance = distanceKm(group);

        return switch (group.getTransportMode()) {
            case CAR -> distance * CAR_KG_CO2_PER_KM;
            case CARPOOL -> (distance * CAR_KG_CO2_PER_KM) / Math.max(memberCount(group, memberCounts), 1);
            case PUBLIC_TRANSPORT -> distance * PUBLIC_TRANSPORT_KG_CO2_PER_KM;
            case BIKE, WALK -> distance * ZERO_EMISSIONS_KG_CO2_PER_KM;
        };
    }

    private double calculateBaselineForGroup(TravelGroup group) {
        return calculateBaselineForGroup(group, Map.of());
    }

    private double calculateBaselineForGroup(TravelGroup group, Map<UUID, Long> memberCounts) {
        return distanceKm(group) * CAR_KG_CO2_PER_KM * memberCount(group, memberCounts);
    }

    private double calculateActualForGroup(TravelGroup group) {
        return calculateActualForGroup(group, Map.of());
    }

    private double calculateActualForGroup(TravelGroup group, Map<UUID, Long> memberCounts) {
        if (group == null || group.getTransportMode() == null) {
            return 0;
        }

        double distance = distanceKm(group);
        long members = memberCount(group, memberCounts);

        return switch (group.getTransportMode()) {
            case CAR -> distance * CAR_KG_CO2_PER_KM * members;
            case CARPOOL -> members == 0 ? 0 : distance * CAR_KG_CO2_PER_KM;
            case PUBLIC_TRANSPORT -> distance * PUBLIC_TRANSPORT_KG_CO2_PER_KM * members;
            case BIKE, WALK -> 0;
        };
    }

    private long memberCount(TravelGroup group) {
        return memberCount(group, Map.of());
    }

    private long memberCount(TravelGroup group, Map<UUID, Long> memberCounts) {
        if (group == null) {
            return 0;
        }

        if (memberCounts.containsKey(group.getGroupId())) {
            return memberCounts.get(group.getGroupId());
        }

        // Fallback for old single group calls that do not pass the preloaded map
        return travelGroupMemberRepository.countByGroup(group);
    }

    private Map<UUID, Long> memberCountsByGroupId(List<TravelGroup> groups) {
        // Start with zero because groups without members are not returned by the count query
        Map<UUID, Long> memberCounts = groups.stream()
                .collect(Collectors.toMap(
                        TravelGroup::getGroupId,
                        group -> 0L,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        if (groups.isEmpty()) {
            return memberCounts;
        }

        // Update the zero filled map with real grouped totals
        travelGroupMemberRepository.countMembersByGroupIn(groups)
                .forEach(row -> memberCounts.put((UUID) row[0], (Long) row[1]));

        return memberCounts;
    }

    private double distanceKm(TravelGroup group) {
        if (group == null || group.getActivity() == null || group.getActivity().getDistanceKm() == null) {
            return DEFAULT_DISTANCE_KM;
        }

        return group.getActivity().getDistanceKm();
    }

    private String formatPeriod(LocalDate date, Co2Period period) {
        return switch (period) {
            case DAILY -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case MONTHLY -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case QUARTERLY -> date.getYear() + " Q" + (((date.getMonthValue() - 1) / 3) + 1);
        };
    }

    private String timeBucket(LocalTime time) {
        int hour = time.getHour();

        if (hour >= 6 && hour < 12) {
            return message("analytics.time.morning");
        }

        if (hour >= 12 && hour < 17) {
            return message("analytics.time.daytime");
        }

        if (hour >= 17 && hour < 22) {
            return message("analytics.time.evening");
        }

        return message("analytics.time.night");
    }

    private String displayMode(TransportMode mode) {
        if (mode == null) {
            return "Unknown";
        }

        return switch (mode) {
            case CAR -> message("transportMode.car");
            case CARPOOL -> message("transportMode.carpool");
            case BIKE -> message("transportMode.bike");
            case WALK -> message("transportMode.walk");
            case PUBLIC_TRANSPORT -> message("transportMode.publicTransport");
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double percentage(double numerator, double denominator) {
        if (denominator <= 0) {
            return 0;
        }

        return round((numerator / denominator) * 100);
    }

    private String message(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}