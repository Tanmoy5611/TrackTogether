package TrackTogether.controller;

import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.service.TravelGroupService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/travelgroups")
public class TravelGroupController {

    private final TravelGroupService travelGroupService;

    public TravelGroupController(TravelGroupService travelGroupService) {
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
}