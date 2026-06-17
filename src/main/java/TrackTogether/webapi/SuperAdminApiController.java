package TrackTogether.webapi;

import TrackTogether.controller.ModelView.ActivityView;
import TrackTogether.controller.ModelView.UserView;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.service.ActivityService;
import TrackTogether.service.SuperAdminService;
import TrackTogether.webapi.dto.UpdateUserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/super_admin/api")
public class SuperAdminApiController {
    private final SuperAdminService superAdminService;
    private final ActivityService activityService;

    public SuperAdminApiController(SuperAdminService superAdminService,
                                   ActivityService activityService){
        this.superAdminService = superAdminService;
        this.activityService = activityService;

    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<UserView> getUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role
    ) {
        return superAdminService.findAllWithFilters(name, role);
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('MODERATOR')")
    public List<ActivityView> getActivities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String timing
    ) {
        return superAdminService.findAllActivitiesWithFilters(search, timing);
    }

    @PatchMapping("/activities/{id}/verification")
    @PreAuthorize("hasRole('MODERATOR')")
    public ActivityView updateActivityVerification(
            @PathVariable UUID id,
            @RequestParam ActivityVerificationStatus status
    ) {
        return superAdminService.updateActivityVerification(id, status);
    }

    @DeleteMapping("/activities/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<?> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/user/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request
    ) {
        superAdminService.updateUser(id, request.getStatus(), request.getRole());

        return ResponseEntity.ok().build();
    }
}