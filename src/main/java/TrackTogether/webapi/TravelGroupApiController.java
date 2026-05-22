package TrackTogether.webapi;

import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.service.FriendMatchingService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.service.delijn.DeLijnService;
import TrackTogether.webapi.dto.JoinResultDto;
import TrackTogether.webapi.dto.TravelGroupDto;
import TrackTogether.webapi.dto.TravelGroupRequestDto;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/travelgroups")
public class TravelGroupApiController {

    private final TravelGroupService service;
    private final TravelGroupMapper mapper;
    private final FriendMatchingService friendMatchingService;
    private final DeLijnService deLijnService;

    // Wires travel group API endpoints with group, matching, mapping, and De Lijn services
    public TravelGroupApiController(TravelGroupService service,
                                    TravelGroupMapper mapper,
                                    FriendMatchingService friendMatchingService,
                                    DeLijnService deLijnService) {
        this.service = service;
        this.mapper = mapper;
        this.friendMatchingService = friendMatchingService;
        this.deLijnService = deLijnService;
    }

    // Returns suggested travel groups for the current user, optionally filtered by activity and transport mode
    @GetMapping("/suggestions")
    public List<TravelFriendSuggestionDto> getSuggestions(
            @RequestParam(required = false) UUID activityId,
            @RequestParam(required = false) TransportMode transportMode) {

        return friendMatchingService.suggestTravelGroupsForCurrentUser(activityId, transportMode);
    }

    // Creates a travel group from the API request body
    @PostMapping
    public TravelGroupDto createTravelGroup(@Valid @RequestBody TravelGroupRequestDto request) {

        TravelGroup group = service.createTravelGroup(
                request.getActivityId(),
                request.getMaxMembers(),
                request.getLocation(),
                request.getDepartureLocation(),
                request.getDepartureLatitude(),
                request.getDepartureLongitude(),
                request.getTransportMode(),
                request.getDepartureTime(),
                request.getEstimatedArrivalTime()
        );


        return mapper.toDto(group);
    }

    // Returns every travel group as API DTOs
    @GetMapping
    public List<TravelGroupDto> getAllTravelGroups() {
        return service.getAllTravelGroups()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    // Builds De Lijn route suggestions for a travel group and optional user-selected route points
    @GetMapping("/{groupId}/route-suggestions")
    public TravelGroupRouteSuggestionsDto getRouteSuggestions(@PathVariable UUID groupId,
                                                              @RequestParam(defaultValue = "4") int maxResults,
                                                              @RequestParam(required = false) Double originLatitude,
                                                              @RequestParam(required = false) Double originLongitude,
                                                              @RequestParam(required = false) String originLabel,
                                                              @RequestParam(required = false) Double destinationLatitude,
                                                              @RequestParam(required = false) Double destinationLongitude,
                                                              @RequestParam(required = false) String destinationLabel,
                                                              @RequestParam(required = false)
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                              LocalDateTime departureTime) {
        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        TravelGroup group = service.getTravelGroupById(groupId);
        Double arrivalLatitude = group.getArrivalLatitude() != null
                ? group.getArrivalLatitude()
                : group.getActivity().getLatitude();
        Double arrivalLongitude = group.getArrivalLongitude() != null
                ? group.getArrivalLongitude()
                : group.getActivity().getLongitude();
        Double routeOriginLatitude = originLatitude != null ? originLatitude : group.getDepartureLatitude();
        Double routeOriginLongitude = originLongitude != null ? originLongitude : group.getDepartureLongitude();
        Double routeDestinationLatitude = destinationLatitude != null ? destinationLatitude : arrivalLatitude;
        Double routeDestinationLongitude = destinationLongitude != null ? destinationLongitude : arrivalLongitude;
        String routeOriginLabel = hasText(originLabel) ? originLabel : group.getDepartureLocation();
        String routeDestinationLabel = hasText(destinationLabel) ? destinationLabel : group.getActivity().getLocation();
        LocalDateTime routeDepartureTime = departureTime != null ? departureTime : group.getDepartureTime();

        if (group.getTransportMode() != TransportMode.PUBLIC_TRANSPORT) {
            return new TravelGroupRouteSuggestionsDto(
                    false,
                    deLijnService.isConfigured(),
                    "Route suggestions are only available for public transport groups.",
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    "De Lijn covers buses and trams.",
                    List.of()
            );
        }

        if (!hasCoordinates(routeOriginLatitude, routeOriginLongitude)
                || !hasCoordinates(routeDestinationLatitude, routeDestinationLongitude)) {
            return new TravelGroupRouteSuggestionsDto(
                    true,
                    deLijnService.isConfigured(),
                    "Add departure and activity coordinates to get De Lijn route suggestions.",
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    "De Lijn covers buses and trams.",
                    List.of()
            );
        }

        if (!deLijnService.isConfigured()) {
            return new TravelGroupRouteSuggestionsDto(
                    true,
                    false,
                    "De Lijn API key is not configured.",
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    "De Lijn covers buses and trams.",
                    List.of()
            );
        }

        List<DeLijnRouteOptionDto> options = deLijnService.getRouteOptions(
                        routeOriginLatitude,
                        routeOriginLongitude,
                        routeOriginLabel,
                        routeDestinationLatitude,
                        routeDestinationLongitude,
                        routeDestinationLabel,
                        routeDepartureTime,
                        group.getEstimatedArrivalTime()
                )
                .stream()
                // Sort before balancing so the final list keeps the earliest useful departures
                .sorted(Comparator.comparing(
                        DeLijnRouteOptionDto::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        return new TravelGroupRouteSuggestionsDto(
                true,
                true,
                routeSuggestionMessage(options),
                routeOriginLabel,
                routeOriginLatitude,
                routeOriginLongitude,
                routeDestinationLabel,
                routeDestinationLatitude,
                routeDestinationLongitude,
                "De Lijn covers buses and trams.",
                balancedTransitOptions(options, maxResults)
        );
    }

    // Joins the current user to a travel group or creates a join request when approval is enabled
    @PostMapping("/{groupId}/join")
    public JoinResultDto joinTravelGroup(@PathVariable UUID groupId) {
        boolean approvalRequired = service.isJoinApprovalRequired();
        service.joinTravelGroup(groupId);
        TravelGroup group = service.getTravelGroupById(groupId);
        long memberCount = service.getMemberCount(group);
        return new JoinResultDto(
                memberCount,
                group.getMaxMembers(),
                !approvalRequired,
                approvalRequired,
                approvalRequired
                        ? "Join request sent. The group owner can accept or reject it."
                        : "You're going too!"
        );
    }

    // Accepts a pending join request by id
    @PostMapping("/requests/{requestId}/accept")
    public void acceptJoinRequest(@PathVariable Integer requestId) {
        service.acceptJoinRequest(requestId);
    }

    // Rejects a pending join request by id
    @PostMapping("/requests/{requestId}/reject")
    public void rejectJoinRequest(@PathVariable Integer requestId) {
        service.rejectJoinRequest(requestId);
    }

    // Transfers ownership to another member of the group
    @PostMapping("/{groupId}/ownership")
    public TravelGroupDto transferOwnership(@PathVariable UUID groupId,
                                            @RequestParam UUID newOwnerId) {
        return mapper.toDto(service.transferOwnership(groupId, newOwnerId));
    }

    // Removes the current user from a travel group
    @DeleteMapping("/{groupId}/leave")
    public void leaveTravelGroup(@PathVariable UUID groupId) {

        service.leaveTravelGroup(groupId);
    }

    // Deletes a travel group owned by the current user
    @DeleteMapping("/{groupId}")
    public void deleteOwnedTravelGroup(@PathVariable UUID groupId) {
        service.deleteOwnedTravelGroup(groupId);
    }

    // Validates latitude/longitude ranges before calling route APIs
    private static boolean hasCoordinates(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    // Checks whether a request parameter contains usable text
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Chooses the user-facing message for the route suggestions response
    private String routeSuggestionMessage(List<DeLijnRouteOptionDto> options) {
        if (!options.isEmpty()) {
            if (deLijnService.hasRouteOptionsEndpoint()) {
                return "De Lijn bus/tram route suggestions loaded.";
            }

            boolean realtimeOnly = options.stream().allMatch(DeLijnRouteOptionDto::isRealtime);
            return realtimeOnly
                    ? "Live De Lijn bus/tram departures near the selected start loaded."
                    : "Scheduled De Lijn bus/tram departures for the selected date and time loaded.";
        }

        if (!deLijnService.hasRouteOptionsEndpoint() && !deLijnService.hasNearbyStopsEndpoint()) {
            return "De Lijn is configured, but no route planner or nearby stops endpoint is configured yet.";
        }

        if (deLijnService.hasScheduledDeparturesEndpoint()) {
            return "No scheduled De Lijn bus/tram departures were found near the selected start for this date and time.";
        }

        return "No De Lijn bus/tram departures were found near the selected start.";
    }

    // Keeps the result set balanced so a tram option is not hidden by many bus options, or the reverse
    private static List<DeLijnRouteOptionDto> balancedTransitOptions(List<DeLijnRouteOptionDto> options, int maxResults) {
        if (options.size() <= maxResults) {
            return options;
        }

        Set<DeLijnRouteOptionDto> selected = new LinkedHashSet<>();
        addFirstTransportType(options, selected, "BUS");
        addFirstTransportType(options, selected, "TRAM");

        for (DeLijnRouteOptionDto option : options) {
            // LinkedHashSet keeps insertion order while avoiding duplicates from the bus/tram seed picks
            if (selected.size() >= maxResults) {
                break;
            }
            selected.add(option);
        }

        return new ArrayList<>(selected).stream()
                .sorted(Comparator.comparing(
                        DeLijnRouteOptionDto::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    // Adds the earliest option for one transport type when it exists
    private static void addFirstTransportType(List<DeLijnRouteOptionDto> options,
                                              Set<DeLijnRouteOptionDto> selected,
                                              String transportType) {
        options.stream()
                .filter(option -> transportType.equalsIgnoreCase(option.getTransportType()))
                .findFirst()
                .ifPresent(selected::add);
    }
}