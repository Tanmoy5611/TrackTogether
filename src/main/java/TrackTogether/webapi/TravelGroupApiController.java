package TrackTogether.webapi;

import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.service.TravelGroupService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/travelgroups")
public class TravelGroupApiController {

    private final TravelGroupService travelGroupService;

    public TravelGroupApiController(TravelGroupService travelGroupService) {
        this.travelGroupService = travelGroupService;
    }

    // Endpoint to create a new travel group for an activity
    @PostMapping
    public TravelGroup createGroup(@RequestParam UUID activityId,
                                   @RequestParam Integer maxMembers,
                                   @RequestParam String location,
                                   @RequestParam TransportMode mode) {

        // Delegate the creation logic to the service layer
        return travelGroupService.createTravelGroup(
                activityId,
                maxMembers,
                location,
                mode
        );
    }

    // Endpoint that allows a member to join a travel group
    @PostMapping("/{groupId}/join")
    public void joinTravelGroup(@PathVariable UUID groupId,
                                @RequestParam UUID memberId) {

        travelGroupService.joinTravelGroup(groupId, memberId);
    }


    @DeleteMapping("/{groupId}/leave")
    public void leaveTravelGroup(@PathVariable UUID groupId,
                                 @RequestParam UUID memberId) {

        travelGroupService.leaveTravelGroup(groupId, memberId);
    }
}