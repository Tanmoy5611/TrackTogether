package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import TrackTogether.repository.ActivityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TravelGroupService travelGroupService;
    private final CurrentUserService currentUserService;
    private final ActivityPolicyService activityPolicyService;

    public ActivityService(ActivityRepository activityRepository,
                           TravelGroupService travelGroupService,
                           CurrentUserService currentUserService,
                           ActivityPolicyService activityPolicyService) {
        this.activityRepository = activityRepository;
        this.travelGroupService = travelGroupService;
        this.currentUserService = currentUserService;
        this.activityPolicyService = activityPolicyService;
    }

    public List<Activity> getAllActivities() {
        Member currentUser = currentUserService.getCurrentUser();
        return activityRepository.findAllByVerificationStatusOrCreator(
                ActivityVerificationStatus.APPROVED,
                currentUser
        );
    }

    public Activity getActivityById(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!activityPolicyService.isVisibleTo(activity, currentUserService.getCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Activity is not public yet");
        }

        return activity;
    }

    public Activity save(Activity activity) {
        activity.setCreator(currentUserService.getCurrentUser());
        activity.setVerificationStatus(ActivityVerificationStatus.PENDING);
        return activityRepository.save(activity);
    }

    public Activity createActivity(Activity activity) {
        if (activity.getCreator() == null) {
            activity.setCreator(currentUserService.getCurrentUser());
        }
        activity.setVerificationStatus(ActivityVerificationStatus.PENDING);
        return activityRepository.save(activity);
    }

    public void deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        travelGroupService.deleteTravelGroupsForActivity(id);
        activityRepository.delete(activity);
    }

    public Set<UUID> getKdgActivityIds(Collection<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Set.of();
        }

        return activities.stream()
                .filter(activityPolicyService::isKdgActivity)
                .map(Activity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean isKdgActivity(Activity activity) {
        return activityPolicyService.isKdgActivity(activity);
    }
}