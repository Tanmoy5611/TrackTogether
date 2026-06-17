package TrackTogether.webapi;

import TrackTogether.domain.Activity;
import TrackTogether.service.ActivityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
public class ActivityApiController {

    private final ActivityService activityService;

    public ActivityApiController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public Activity createActivity(@RequestBody Activity activity) {
        return activityService.save(activity);
    }
}