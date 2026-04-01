package TrackTogether.webapi;

import TrackTogether.domain.TravelGroup;
import TrackTogether.service.TravelGroupService;
import TrackTogether.webapi.dto.TravelGroupDto;
import TrackTogether.webapi.dto.TravelGroupRequestDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
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
    public TravelGroupDto createTravelGroup(@RequestBody TravelGroupRequestDto request) {

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

    // Join TravelGroup (add member)
    @PostMapping("/{groupId}/members/{memberId}")
    public void joinTravelGroup(@PathVariable UUID groupId,
                                @PathVariable UUID memberId) {

        service.joinTravelGroup(groupId, memberId);
    }

    // Leave TravelGroup (remove member)
    @DeleteMapping("/{groupId}/members/{memberId}")
    public void leaveTravelGroup(@PathVariable UUID groupId,
                                 @PathVariable UUID memberId) {

        service.leaveTravelGroup(groupId, memberId);
    }
}