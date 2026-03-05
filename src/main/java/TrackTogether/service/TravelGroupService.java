package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TravelGroupService {

    // Repository used to store and retrieve TravelGroup entities
    private final TravelGroupRepository travelGroupRepository;

    // Repository used to store and retrieve TravelGroup entities
    private final ActivityRepository activityRepository;

    public TravelGroupService(TravelGroupRepository travelGroupRepository,
                              ActivityRepository activityRepository) {
        this.travelGroupRepository = travelGroupRepository;
        this.activityRepository = activityRepository;
    }

    // Creates a new TravelGroup for a given activity
    public TravelGroup createTravelGroup(UUID activityId,
                                         Integer maxMembers,
                                         String location,
                                         TransportMode mode) {

        // Retrieve the activity from the database
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // Create a new TravelGroup with the provided information
        TravelGroup group = new TravelGroup(maxMembers, location, mode);

        // Link the group to the activity
        group.setActivity(activity);

        // Save the group in the database
        return travelGroupRepository.save(group);
    }
}