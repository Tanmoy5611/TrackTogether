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

    // Join directly or create a join request, depending on the system setting
    @PostMapping("/{groupId}/join")
    public void joinTravelGroup(@PathVariable UUID groupId) {

        service.joinTravelGroup(groupId);
    }

    // Accept a join request
    @PostMapping("/requests/{requestId}/accept")
    public void acceptJoinRequest(@PathVariable Integer requestId) {
        service.acceptJoinRequest(requestId);
    }

    // reject a join request
    @PostMapping("/requests/{requestId}/reject")
    public void rejectJoinRequest(@PathVariable Integer requestId) {
        service.rejectJoinRequest(requestId);
    }

    // Transfer ownership to another member already in the TravelGroup
    @PostMapping("/{groupId}/ownership")
    public TravelGroupDto transferOwnership(@PathVariable UUID groupId,
                                            @RequestParam UUID newOwnerId) {
        return mapper.toDto(service.transferOwnership(groupId, newOwnerId));
    }

    // Leave TravelGroup as the logged-in member
    @DeleteMapping("/{groupId}/leave")
    public void leaveTravelGroup(@PathVariable UUID groupId) {

        service.leaveTravelGroup(groupId);
    }

    // Delete an owner-only TravelGroup as the logged-in owner
    @DeleteMapping("/{groupId}")
    public void deleteOwnedTravelGroup(@PathVariable UUID groupId) {
        service.deleteOwnedTravelGroup(groupId);
    }
}