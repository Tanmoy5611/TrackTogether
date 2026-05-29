package TrackTogether.webapi;

import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.service.FriendMatchingService;
import TrackTogether.service.TravelGroupRouteSuggestionService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.webapi.dto.JoinResultDto;
import TrackTogether.webapi.dto.TravelGroupDto;
import TrackTogether.webapi.dto.TravelGroupRequestDto;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/travelgroups")
public class TravelGroupApiController {

    private final TravelGroupService travelGroupService;
    private final TravelGroupMapper mapper;
    private final FriendMatchingService friendMatchingService;
    private final TravelGroupRouteSuggestionService routeSuggestionService;
    private final MessageSource messageSource;

    // Wires travel group API endpoints with group, matching, mapping, and route services
    public TravelGroupApiController(TravelGroupService travelGroupService,
                                    TravelGroupMapper mapper,
                                    FriendMatchingService friendMatchingService,
                                    TravelGroupRouteSuggestionService routeSuggestionService,
                                    MessageSource messageSource) {
        this.travelGroupService = travelGroupService;
        this.mapper = mapper;
        this.friendMatchingService = friendMatchingService;
        this.routeSuggestionService = routeSuggestionService;
        this.messageSource = messageSource;
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

        TravelGroup group = travelGroupService.createTravelGroup(
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
        return travelGroupService.getAllTravelGroups()
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
        return routeSuggestionService.getRouteSuggestions(
                groupId,
                maxResults,
                originLatitude,
                originLongitude,
                originLabel,
                destinationLatitude,
                destinationLongitude,
                destinationLabel,
                departureTime
        );
    }

    // Joins the current user to a travel group or creates a join request when approval is enabled
    @PostMapping("/{groupId}/join")
    public JoinResultDto joinTravelGroup(@PathVariable UUID groupId) {
        boolean approvalRequired = travelGroupService.isJoinApprovalRequired();
        travelGroupService.joinTravelGroup(groupId);
        TravelGroup group = travelGroupService.getTravelGroupById(groupId);
        long memberCount = travelGroupService.getMemberCount(group);
        return new JoinResultDto(
                memberCount,
                group.getMaxMembers(),
                !approvalRequired,
                approvalRequired,
                approvalRequired
                        ? message("flash.travelGroup.joinRequestSent")
                        : message("html.you.re.going.too")
        );
    }

    // Accepts a pending join request by id
    @PostMapping("/requests/{requestId}/accept")
    public void acceptJoinRequest(@PathVariable Integer requestId) {
        travelGroupService.acceptJoinRequest(requestId);
    }

    // Rejects a pending join request by id
    @PostMapping("/requests/{requestId}/reject")
    public void rejectJoinRequest(@PathVariable Integer requestId) {
        travelGroupService.rejectJoinRequest(requestId);
    }

    // Transfers ownership to another member of the group
    @PostMapping("/{groupId}/ownership")
    public TravelGroupDto transferOwnership(@PathVariable UUID groupId,
                                            @RequestParam UUID newOwnerId) {
        return mapper.toDto(travelGroupService.transferOwnership(groupId, newOwnerId));
    }

    // Removes the current user from a travel group
    @DeleteMapping("/{groupId}/leave")
    public void leaveTravelGroup(@PathVariable UUID groupId) {

        travelGroupService.leaveTravelGroup(groupId);
    }

    // Deletes a travel group owned by the current user
    @DeleteMapping("/{groupId}")
    public void deleteOwnedTravelGroup(@PathVariable UUID groupId) {
        travelGroupService.deleteOwnedTravelGroup(groupId);
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}