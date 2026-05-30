package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.dto.delijn.DeLijnStopDto;
import TrackTogether.repository.JoinRequestRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import TrackTogether.service.delijn.DeLijnService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendMatchingService {

    private static final double NEARBY_DISTANCE_KM = 2.0;
    private static final double ACCEPTABLE_DISTANCE_KM = 5.0;
    private static final int MAX_DELIJN_COMPARISONS_PER_REQUEST = 6;
    private static final int DELIJN_DEPARTURE_WINDOW_MINUTES = 30;

    private final TravelGroupRepository travelGroupRepository;
    private final TravelGroupMemberRepository travelGroupMemberRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final CurrentUserService currentUserService;
    private final DeLijnService deLijnService;
    private final ActivityPolicyService activityPolicyService;
    private final MessageSource messageSource;

    public FriendMatchingService(TravelGroupRepository travelGroupRepository,
                                  TravelGroupMemberRepository travelGroupMemberRepository,
                                  JoinRequestRepository joinRequestRepository,
                                  CurrentUserService currentUserService,
                                  DeLijnService deLijnService,
                                  ActivityPolicyService activityPolicyService,
                                  MessageSource messageSource) {
        this.travelGroupRepository = travelGroupRepository;
        this.travelGroupMemberRepository = travelGroupMemberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.currentUserService = currentUserService;
        this.deLijnService = deLijnService;
        this.activityPolicyService = activityPolicyService;
        this.messageSource = messageSource;
    }

    public List<TravelFriendSuggestionDto> suggestTravelGroupsForCurrentUser() {
        return suggestTravelGroupsForCurrentUser(null, null);
    }

    public List<TravelFriendSuggestionDto> suggestTravelGroupsForCurrentUser(UUID activityId,
                                                                             TransportMode transportMode) {
        Member currentUser = currentUserService.getCurrentUser();

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
                .map(Activity::getId)
                .collect(Collectors.toSet());
        List<LocalTime> joinedDepartureTimes = memberships.stream()
                .map(TravelGroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getDepartureTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalTime)
                .toList();
        MatchingContext matchingContext = new MatchingContext();

        List<TravelGroup> candidateGroups = travelGroupRepository.findAll()
                .stream()
                .filter(Objects::nonNull)
                .filter(group -> activityPolicyService.isVisibleTo(group.getActivity(), currentUser))
                .filter(group -> !joinedGroupIds.contains(group.getGroupId()))
                .filter(group -> !isOwnedBy(group, currentUser))
                .filter(FriendMatchingService::isUpcomingOrUndated)
                .filter(group -> activityId == null || isGroupForActivity(group, activityId))
                .filter(group -> transportMode == null || transportMode == group.getTransportMode())
                .toList();

        Set<UUID> pendingJoinRequestGroupIds = pendingJoinRequestGroupIds(currentUser, candidateGroups);
        // Preload candidate counts so scoring does not count every group one by one
        Map<UUID, Long> memberCounts = memberCountsByGroupId(candidateGroups);

        return candidateGroups.stream()
                .filter(group -> !pendingJoinRequestGroupIds.contains(group.getGroupId()))
                .map(group -> toSuggestion(
                        group,
                        currentUser,
                        memberCounts.getOrDefault(group.getGroupId(), 0L),
                        joinedActivityIds,
                        joinedDepartureTimes,
                        activityId,
                        transportMode,
                        matchingContext
                ))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(TravelFriendSuggestionDto::getScore).reversed()
                        .thenComparing(TravelFriendSuggestionDto::getDepartureTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(suggestion -> normalize(suggestion.getActivityName())))
                .toList();
    }

    private Set<UUID> pendingJoinRequestGroupIds(Member currentUser, List<TravelGroup> groups) {
        if (groups.isEmpty()) {
            return Set.of();
        }

        List<JoinRequest> joinRequests = joinRequestRepository.findAllByMemberAndGroupIn(currentUser, groups);
        if (joinRequests == null) {
            return Set.of();
        }

        return joinRequests.stream()
                .filter(Objects::nonNull)
                .filter(joinRequest -> joinRequest.getStatus() == JoinRequestStatus.PENDING)
                .map(JoinRequest::getGroup)
                .filter(Objects::nonNull)
                .map(TravelGroup::getGroupId)
                .collect(Collectors.toSet());
    }

    private TravelFriendSuggestionDto toSuggestion(TravelGroup group,
                                                   Member currentUser,
                                                   long memberCount,
                                                   Set<UUID> joinedActivityIds,
                                                   List<LocalTime> joinedDepartureTimes,
                                                   UUID requestedActivityId,
                                                   TransportMode requestedTransportMode,
                                                   MatchingContext matchingContext) {
        if (group.getMaxMembers() == null || !group.hasAvailableSpots(memberCount)) {
            return null;
        }

        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (requestedActivityId != null && isGroupForActivity(group, requestedActivityId)) {
            score += 50;
            reasons.add(message("matching.reason.sameEvent"));
        } else if (group.getActivity() != null && joinedActivityIds.contains(group.getActivity().getId())) {
            score += 50;
            reasons.add(message("matching.reason.sameEvent"));
        }

        TransportMode preferredMode = currentUser.getPreferredTransportMode();
        if (requestedTransportMode != null && requestedTransportMode == group.getTransportMode()) {
            score += 40;
            reasons.add(reasonForTransportMode(group.getTransportMode()));
        } else if (preferredMode != null && group.getTransportMode() != null) {
            if (preferredMode == group.getTransportMode()) {
                score += 40;
                reasons.add(reasonForTransportMode(group.getTransportMode()));
            } else if (isCompatibleTransportMode(preferredMode, group.getTransportMode())) {
                score += 25;
                reasons.add(message("matching.reason.compatibleTransport"));
            }
        }

        Double departureDistance = distanceKm(
                currentUser.getDefaultLatitude(),
                currentUser.getDefaultLongitude(),
                group.getDepartureLatitude(),
                group.getDepartureLongitude()
        );
        if (departureDistance != null) {
            if (departureDistance <= NEARBY_DISTANCE_KM) {
                score += 35;
                reasons.add(message("matching.reason.nearLocation"));
            } else if (departureDistance <= ACCEPTABLE_DISTANCE_KM) {
                score += 20;
                reasons.add(message("matching.reason.closeLocation"));
            }
        } else if (sameText(currentUser.getDefaultDepartureLocation(), departureLocationForMatching(group))) {
            score += 20;
            reasons.add(message("matching.reason.defaultDeparture"));
        }

        if (isCloseToKnownDepartureTime(group, joinedDepartureTimes)) {
            score += 15;
            reasons.add(message("matching.reason.sameTime"));
        }

        PublicTransportMatch publicTransportMatch = scorePublicTransportRoute(
                group,
                currentUser,
                requestedTransportMode,
                matchingContext
        );
        score += publicTransportMatch.score();
        reasons.addAll(publicTransportMatch.reasons());

        if (reasons.isEmpty()) {
            score += 5;
            reasons.add(message("matching.reason.availableSeats"));
        }

        return new TravelFriendSuggestionDto(
                group.getGroupId(),
                group.getActivity() != null ? group.getActivity().getId() : null,
                group.getActivity() != null ? group.getActivity().getName() : null,
                departureLocationForMatching(group),
                group.getDepartureTime(),
                group.getTransportMode(),
                memberCount,
                group.getMaxMembers(),
                score,
                reasons
        );
    }

    private Map<UUID, Long> memberCountsByGroupId(List<TravelGroup> groups) {
        if (groups.isEmpty()) {
            return Map.of();
        }

        // The repository returns one row per group that has members
        return travelGroupMemberRepository.countMembersByGroupIn(groups)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    private String reasonForTransportMode(TransportMode transportMode) {
        if (transportMode == TransportMode.PUBLIC_TRANSPORT) {
            return message("matching.reason.publicTransport");
        }

        return message("matching.reason.sameTransport");
    }

    private PublicTransportMatch scorePublicTransportRoute(TravelGroup group,
                                                           Member currentUser,
                                                           TransportMode requestedTransportMode,
                                                           MatchingContext matchingContext) {
        if (!shouldUsePublicTransportMatching(group, currentUser, requestedTransportMode)) {
            return PublicTransportMatch.empty();
        }

        Double destinationLatitude = destinationLatitude(group);
        Double destinationLongitude = destinationLongitude(group);
        if (currentUser.getDefaultLatitude() == null
                || currentUser.getDefaultLongitude() == null
                || group.getDepartureLatitude() == null
                || group.getDepartureLongitude() == null
                || destinationLatitude == null
                || destinationLongitude == null) {
            return PublicTransportMatch.empty();
        }

        if (!matchingContext.reserveDeLijnComparison()) {
            return PublicTransportMatch.empty();
        }

        LocalDateTime departureTime = group.getDepartureTime();
        LocalDateTime arriveBefore = arriveBefore(group);
        RouteRequest userRouteRequest = new RouteRequest(
                currentUser.getDefaultLatitude(),
                currentUser.getDefaultLongitude(),
                destinationLatitude,
                destinationLongitude,
                departureTime,
                arriveBefore
        );
        RouteRequest groupRouteRequest = new RouteRequest(
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                destinationLatitude,
                destinationLongitude,
                departureTime,
                arriveBefore
        );

        List<DeLijnRouteOptionDto> userRoutes = matchingContext.routeOptions(userRouteRequest);
        List<DeLijnRouteOptionDto> groupRoutes = matchingContext.routeOptions(groupRouteRequest);
        if (userRoutes.isEmpty() || groupRoutes.isEmpty()) {
            return PublicTransportMatch.empty();
        }

        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        int score = 0;

        if (hasSameOriginStop(userRoutes, groupRoutes)) {
            score += 30;
            reasons.add(message("matching.reason.sameDelijnStop"));
        }

        if (hasSharedLine(userRoutes, groupRoutes)) {
            score += 25;
            reasons.add(message("matching.reason.compatibleDelijnRoute"));
        }

        if (arrivesBeforeEvent(userRoutes) || arrivesBeforeEvent(groupRoutes)) {
            score += 15;
            reasons.add(message("matching.reason.arrivesBeforeEvent"));
        }

        if (hasSimilarRouteDepartureTime(userRoutes, groupRoutes)) {
            score += 15;
            reasons.add(message("matching.reason.similarDeparture"));
        }

        return new PublicTransportMatch(score, List.copyOf(reasons));
    }

    private boolean shouldUsePublicTransportMatching(TravelGroup group,
                                                     Member currentUser,
                                                     TransportMode requestedTransportMode) {
        return deLijnService != null
                && deLijnService.isConfigured()
                && group.getTransportMode() == TransportMode.PUBLIC_TRANSPORT
                && (requestedTransportMode == null
                || currentUser.getPreferredTransportMode() == TransportMode.PUBLIC_TRANSPORT
                || requestedTransportMode == TransportMode.PUBLIC_TRANSPORT);
    }

    private static boolean hasSameOriginStop(List<DeLijnRouteOptionDto> userRoutes,
                                             List<DeLijnRouteOptionDto> groupRoutes) {
        Set<String> userOriginStops = userRoutes.stream()
                .map(DeLijnRouteOptionDto::getOriginStop)
                .map(FriendMatchingService::stopKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return groupRoutes.stream()
                .map(DeLijnRouteOptionDto::getOriginStop)
                .map(FriendMatchingService::stopKey)
                .filter(Objects::nonNull)
                .anyMatch(userOriginStops::contains);
    }

    private static boolean hasSharedLine(List<DeLijnRouteOptionDto> userRoutes,
                                         List<DeLijnRouteOptionDto> groupRoutes) {
        Set<String> userLines = userRoutes.stream()
                .flatMap(route -> route.getLineNumbers().stream())
                .map(FriendMatchingService::normalize)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toSet());

        return groupRoutes.stream()
                .flatMap(route -> route.getLineNumbers().stream())
                .map(FriendMatchingService::normalize)
                .filter(line -> !line.isBlank())
                .anyMatch(userLines::contains);
    }

    private static boolean arrivesBeforeEvent(List<DeLijnRouteOptionDto> routes) {
        return routes.stream().anyMatch(DeLijnRouteOptionDto::isArrivesBeforeRequestedTime);
    }

    private static boolean hasSimilarRouteDepartureTime(List<DeLijnRouteOptionDto> userRoutes,
                                                        List<DeLijnRouteOptionDto> groupRoutes) {
        List<LocalDateTime> userDepartureTimes = userRoutes.stream()
                .map(DeLijnRouteOptionDto::getDepartureTime)
                .filter(Objects::nonNull)
                .toList();

        return groupRoutes.stream()
                .map(DeLijnRouteOptionDto::getDepartureTime)
                .filter(Objects::nonNull)
                .anyMatch(groupDepartureTime -> userDepartureTimes.stream()
                        .anyMatch(userDepartureTime -> Math.abs(Duration.between(
                                userDepartureTime,
                                groupDepartureTime
                        ).toMinutes()) <= DELIJN_DEPARTURE_WINDOW_MINUTES));
    }

    private static String stopKey(DeLijnStopDto stop) {
        if (stop == null || isNullOrBlank(stop.getEntityNumber()) || isNullOrBlank(stop.getStopNumber())) {
            return null;
        }

        return normalize(stop.getEntityNumber()) + ":" + normalize(stop.getStopNumber());
    }

    private static boolean isOwnedBy(TravelGroup group, Member currentUser) {
        return group.getOwner() != null
                && group.getOwner().getUserId() != null
                && group.getOwner().getUserId().equals(currentUser.getUserId());
    }

    private static boolean isGroupForActivity(TravelGroup group, UUID activityId) {
        return group.getActivity() != null && activityId.equals(group.getActivity().getId());
    }

    private static boolean isUpcomingOrUndated(TravelGroup group) {
        if (group.getActivity() == null || group.getActivity().getDate() == null) {
            return true;
        }

        LocalDate activityDate = group.getActivity().getDate();
        if (activityDate.isAfter(LocalDate.now())) {
            return true;
        }

        if (activityDate.isBefore(LocalDate.now())) {
            return false;
        }

        LocalTime activityTime = group.getActivity().getTime();
        return activityTime == null || !activityTime.isBefore(LocalTime.now());
    }

    private static String departureLocationForMatching(TravelGroup group) {
        String departureLocation = normalize(group.getDepartureLocation());
        if (!departureLocation.isBlank()) {
            return group.getDepartureLocation().trim();
        }

        String location = normalize(group.getLocation());
        return location.isBlank() ? null : group.getLocation().trim();
    }

    private static Double destinationLatitude(TravelGroup group) {
        if (group.getArrivalLatitude() != null) {
            return group.getArrivalLatitude();
        }

        return group.getActivity() == null ? null : group.getActivity().getLatitude();
    }

    private static Double destinationLongitude(TravelGroup group) {
        if (group.getArrivalLongitude() != null) {
            return group.getArrivalLongitude();
        }

        return group.getActivity() == null ? null : group.getActivity().getLongitude();
    }

    private static LocalDateTime arriveBefore(TravelGroup group) {
        if (group.getEstimatedArrivalTime() != null) {
            return group.getEstimatedArrivalTime();
        }

        if (group.getActivity() == null
                || group.getActivity().getDate() == null
                || group.getActivity().getTime() == null) {
            return null;
        }

        return LocalDateTime.of(group.getActivity().getDate(), group.getActivity().getTime());
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

    private static boolean isNullOrBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    private class MatchingContext {
        private final Map<RouteRequest, List<DeLijnRouteOptionDto>> routeOptionsCache = new HashMap<>();
        private int deLijnComparisons;

        boolean reserveDeLijnComparison() {
            if (deLijnComparisons >= MAX_DELIJN_COMPARISONS_PER_REQUEST) {
                return false;
            }

            deLijnComparisons++;
            return true;
        }

        List<DeLijnRouteOptionDto> routeOptions(RouteRequest request) {
            return routeOptionsCache.computeIfAbsent(request, this::fetchRouteOptions);
        }

        private List<DeLijnRouteOptionDto> fetchRouteOptions(RouteRequest request) {
            try {
                List<DeLijnRouteOptionDto> routeOptions = deLijnService.getRouteOptions(
                        request.originLatitude(),
                        request.originLongitude(),
                        request.destinationLatitude(),
                        request.destinationLongitude(),
                        request.departureTime(),
                        request.arriveBefore()
                );

                return routeOptions == null ? List.of() : routeOptions;
            } catch (RuntimeException exception) {
                return List.of();
            }
        }
    }

    private record RouteRequest(Double originLatitude,
                                Double originLongitude,
                                Double destinationLatitude,
                                Double destinationLongitude,
                                LocalDateTime departureTime,
                                LocalDateTime arriveBefore) {
    }

    private record PublicTransportMatch(int score, List<String> reasons) {
        static PublicTransportMatch empty() {
            return new PublicTransportMatch(0, List.of());
        }
    }
}