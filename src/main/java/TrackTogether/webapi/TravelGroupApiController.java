package TrackTogether.webapi;

import TrackTogether.domain.TravelGroup;
import TrackTogether.service.TravelGroupService;
import TrackTogether.webapi.dto.TravelGroupDto;
import TrackTogether.webapi.dto.TravelGroupRequestDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// REST controller for TravelGroup API endpoints
@RestController
@RequestMapping("/api/travelgroups")
public class TravelGroupApiController {

    private final TravelGroupService service;
    private final TravelGroupMapper mapper;

    // Constructor injection
    public TravelGroupApiController(TravelGroupService service,
                                    TravelGroupMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // Create TravelGroup
    @PostMapping
    public TravelGroupDto createTravelGroup(@Valid @RequestBody TravelGroupRequestDto request) {

        TravelGroup group = service.createTravelGroup(
                request.getActivityId(),
                request.getMaxMembers(),
                request.getLocation(),
                request.getTransportMode()
        );

        return mapper.toDto(group);
    }

    // Get all TravelGroups
    @GetMapping
    public List<TravelGroupDto> getAllTravelGroups() {
        return service.getAllTravelGroups()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    // Request access to a TravelGroup as the logged-in member
    @PostMapping("/{groupId}/join")
    public void joinTravelGroup(@PathVariable UUID groupId) {

        service.requestToJoinTravelGroup(groupId);
    }

    @PostMapping("/requests/{requestId}/accept")
    public void acceptJoinRequest(@PathVariable Integer requestId) {
        service.acceptJoinRequest(requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public void rejectJoinRequest(@PathVariable Integer requestId) {
        service.rejectJoinRequest(requestId);
    }

    // Leave TravelGroup as the logged-in member
    @DeleteMapping("/{groupId}/leave")
    public void leaveTravelGroup(@PathVariable UUID groupId) {

        service.leaveTravelGroup(groupId);
    }
}