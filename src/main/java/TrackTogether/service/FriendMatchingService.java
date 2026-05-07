package TrackTogether.service;

import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendMatchingService {

    private static final double NEARBY_DISTANCE_KM = 2.0;
    private static final double ACCEPTABLE_DISTANCE_KM = 5.0;

    private final TravelGroupRepository travelGroupRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final CurrentUserService currentUserService;

    public FriendMatchingService(TravelGroupRepository travelGroupRepository,
                                 TravelGroupMemberRepository travelGroupMemberRepository,
                                 CurrentUserService currentUserService) {
        this.travelGroupRepository = travelGroupRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.currentUserService = currentUserService;
    }

    public List<TravelFriendSuggestionDto> suggestTravelGroupsForCurrentUser() {
        Member currentUser = currentUserService.getCurrentUser();

        // First collect what the user already joined so we do not suggest it again
        List<TravelGroupMember> memberships = travelGroupMemberRepository.findAllByMember(currentUser);
        Set<UUID> joinedGroupIds = memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
        Set<UUID> joinedActivityIds = memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getActivity)
                .filter(Objects::nonNull)
                .map(activity -> activity.getId())
                .collect(Collectors.toSet());
        List<LocalTime> joinedDepartureTimes = memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getDepartureTime)
                .filter(Objects::nonNull)
                .map(departureTime -> departureTime.toLocalTime())
                .toList();

        return travelGroupRepository.findAll()
                .stream()
                .filter(Objects::nonNull)
                .filter(group -> !joinedGroupIds.contains(group.getGroupId()))
                .map(group -> toSuggestion(group, currentUser, joinedActivityIds, joinedDepartureTimes))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(TravelFriendSuggestionDto::getScore).reversed()
                        .thenComparing(TravelFriendSuggestionDto::getDepartureTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(suggestion -> normalize(suggestion.getActivityName())))
                .toList();
    }

    private TravelFriendSuggestionDto toSuggestion(TravelGroup group,
                                                   Member currentUser,
                                                   Set<UUID> joinedActivityIds,
                                                   List<LocalTime> joinedDepartureTimes) {
        long memberCount = travelGroupMemberRepository.countByGroup(group);
        if (group.getMaxMembers() == null || !group.hasAvailableSpots(memberCount)) {
            return null;
        }

        List<String> reasons = new ArrayList<>();
        int score = 0;

        // Same event is the strongest signal because the user is already going there
        if (group.getActivity() != null && joinedActivityIds.contains(group.getActivity().getId())) {
            score += 50;
            reasons.add("Same event");
        }

        TransportMode preferredMode = currentUser.getPreferredTransportMode();
        if (preferredMode != null && group.getTransportMode() != null) {
            if (preferredMode == group.getTransportMode()) {
                score += 40;
                reasons.add(reasonForTransportMode(group.getTransportMode()));
            } else if (isCompatibleTransportMode(preferredMode, group.getTransportMode())) {
                score += 25;
                reasons.add("Compatible transport mode");
            }
        }

        // Prefer coordinates, but fall back to comparing the location text if coordinates are missing
        Double departureDistance = distanceKm(
                currentUser.getDefaultLatitude(),
                currentUser.getDefaultLongitude(),
                group.getDepartureLatitude(),
                group.getDepartureLongitude()
        );
        if (departureDistance != null) {
            if (departureDistance <= NEARBY_DISTANCE_KM) {
                score += 35;
                reasons.add("Starts near your location");
            } else if (departureDistance <= ACCEPTABLE_DISTANCE_KM) {
                score += 20;
                reasons.add("Starts close to your location");
            }
        } else if (sameText(currentUser.getDefaultDepartureLocation(), group.getDepartureLocation())) {
            score += 20;
            reasons.add("Starts from your default departure location");
        }

        if (isCloseToKnownDepartureTime(group, joinedDepartureTimes)) {
            score += 15;
            reasons.add("Leaves around the same time");
        }

        if (reasons.isEmpty()) {
            score += 5;
            reasons.add("Available seats");
        }

        return new TravelFriendSuggestionDto(
                group.getGroupId(),
                group.getActivity() != null ? group.getActivity().getId() : null,
                group.getActivity() != null ? group.getActivity().getName() : null,
                group.getDepartureLocation() != null ? group.getDepartureLocation() : group.getLocation(),
                group.getDepartureTime(),
                group.getTransportMode(),
                group.getMaxMembers() - memberCount,
                score,
                reasons
        );
    }

    private static String reasonForTransportMode(TransportMode transportMode) {
        if (transportMode == TransportMode.PUBLIC_TRANSPORT) {
            return "Also going by public transport";
        }

        return "Same transport mode";
    }

    private static boolean isCompatibleTransportMode(TransportMode preferredMode, TransportMode groupMode) {
        return (preferredMode == TransportMode.CAR && groupMode == TransportMode.CARPOOL)
                || (preferredMode == TransportMode.CARPOOL && groupMode == TransportMode.CAR)
                || (preferredMode == TransportMode.WALK && groupMode == TransportMode.BIKE)
                || (preferredMode == TransportMode.BIKE && groupMode == TransportMode.WALK);
    }

    private static boolean isCloseToKnownDepartureTime(TravelGroup group, List<LocalTime> joinedDepartureTimes) {
        if (group.getDepartureTime() == null || joinedDepartureTimes.isEmpty()) {
            return false;
        }

        LocalTime candidateTime = group.getDepartureTime().toLocalTime();
        return joinedDepartureTimes.stream()
                .anyMatch(joinedTime -> Math.abs(Duration.between(joinedTime, candidateTime).toMinutes()) <= 60);
    }

    private static Double distanceKm(Double latitudeA, Double longitudeA, Double latitudeB, Double longitudeB) {
        if (latitudeA == null || longitudeA == null || latitudeB == null || longitudeB == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double latitudeDistance = Math.toRadians(latitudeB - latitudeA);
        double longitudeDistance = Math.toRadians(longitudeB - longitudeA);
        double startLatitude = Math.toRadians(latitudeA);
        double endLatitude = Math.toRadians(latitudeB);

        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return earthRadiusKm * centralAngle;
    }

    private static boolean sameText(String left, String right) {
        return !normalize(left).isBlank() && normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}