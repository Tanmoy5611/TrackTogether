package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.repository.ActivityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TravelGroupService travelGroupService;

    public ActivityService(ActivityRepository activityRepository,
                           TravelGroupService travelGroupService) {
        this.activityRepository = activityRepository;
        this.travelGroupService = travelGroupService;
    }

    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    public Activity getActivityById(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    public Activity createActivity(String authId, Activity activity) {
        return activityRepository.save(activity);
    }

    public void deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // Remove linked travel groups first so no group can remain without an activity.
        travelGroupService.deleteTravelGroupsForActivity(id);
        activityRepository.delete(activity);
    }
}