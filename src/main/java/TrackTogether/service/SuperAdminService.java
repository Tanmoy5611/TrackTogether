package TrackTogether.service;

import TrackTogether.controller.ModelView.ActivityView;
import TrackTogether.controller.ModelView.UserView;
import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import TrackTogether.domain.SuperAdmin;
import TrackTogether.domain.User;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.AdminRepository;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.repository.SuperAdminRepository;
import TrackTogether.repository.UserRepository;
import TrackTogether.security.UserSessionService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ModeratorRepository moderatorRepository;
    private final MemberService memberService;
    private final RoleService roleService;
    private final UserSessionService userSessionService;
    private final ActivityPolicyService activityPolicyService;

    public SuperAdminService(SuperAdminRepository superAdminRepository,
                             AdminRepository adminRepository,
                             UserRepository userRepository,
                             ActivityRepository activityRepository,
                             ModeratorRepository moderatorRepository,
                             MemberService memberService,
                             RoleService roleService,
                             UserSessionService userSessionService,
                             ActivityPolicyService activityPolicyService){
        this.superAdminRepository = superAdminRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.moderatorRepository = moderatorRepository;
        this.memberService = memberService;
        this.roleService = roleService;
        this.userSessionService = userSessionService;
        this.activityPolicyService = activityPolicyService;
    }

    public List<SuperAdmin> findAll(){
        return superAdminRepository.findAll();
    }

    public boolean existsByUserId(UUID id){
        return superAdminRepository.existsByUserId(id);
    }

    public boolean isLastSuperAdmin(UUID id) {
        Set<UUID> superAdminIds = superAdminRepository.findAllUserIds();
        return superAdminIds.size() == 1 && superAdminIds.contains(id);
    }

    public List<UserView> findAllWithRole(){
        RoleLookup roleLookup = loadRoleLookup();
        return userRepository.findAll().stream()
                .map(user -> toUserView(user, roleLookup))
                .toList();
    }
    public List<UserView> findAllWithFilters(String name, String role) {
        RoleLookup roleLookup = loadRoleLookup();

        return usersMatchingName(name).stream()
                .map(user -> toUserView(user, roleLookup))
                .filter(user -> {
                    if (role == null || role.isBlank()) return true;
                    return user.getRole().equalsIgnoreCase(role);
                })
                .toList();
    }

    public UserView findByIdWithRole(UUID id){
        Member member = memberService.findById(id);
        return toUserView(member, loadRoleLookup());

    }

    @Transactional
    public void updateUser(UUID id, Boolean status, String role) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        Member user = memberService.findById(id);
        String normalizedRole = normalizeRole(role);
        preventRemovingLastSuperAdmin(id, normalizedRole);
        String previousRole = roleFor(id);
        boolean statusChanged = !Objects.equals(user.getStatus(), status);
        boolean roleChanged = !Objects.equals(previousRole, normalizedRole);

        user.setStatus(status);

        userRepository.save(user);

        if (roleChanged) {
            roleService.updateUserRole(user, normalizedRole);
        }

        if (statusChanged || roleChanged) {
            userSessionService.expireSessionsFor(user);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("MEMBER", "MODERATOR", "ADMIN", "SUPER_ADMIN").contains(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is invalid");
        }

        return normalizedRole;
    }

    private void preventRemovingLastSuperAdmin(UUID id, String normalizedRole) {
        if ("SUPER_ADMIN".equals(normalizedRole)) {
            return;
        }

        List<SuperAdmin> superAdmins = superAdminRepository.findAllForUpdate();
        boolean targetIsSuperAdmin = superAdmins.stream()
                .anyMatch(superAdmin -> superAdmin.getUserId().equals(id));

        if (targetIsSuperAdmin && superAdmins.size() == 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The last super admin must keep the Super Admin role"
            );
        }
    }

    private String roleFor(UUID id) {
        return roleFor(id, loadRoleLookup());
    }

    private String roleFor(UUID id, RoleLookup roleLookup) {
        if (roleLookup.superAdminIds().contains(id)) {
            return "SUPER_ADMIN";
        }

        if (roleLookup.adminIds().contains(id)) {
            return "ADMIN";
        }

        if (roleLookup.moderatorIds().contains(id)) {
            return "MODERATOR";
        }

        return "MEMBER";
    }

    private List<User> usersMatchingName(String name) {
        if (name == null || name.isBlank()) {
            return userRepository.findAll();
        }

        return userRepository.findByNameContainingIgnoreCase(name);
    }

    private UserView toUserView(User user, RoleLookup roleLookup) {
        return new UserView(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                roleFor(user.getUserId(), roleLookup)
        );
    }

    private RoleLookup loadRoleLookup() {
        return new RoleLookup(
                adminRepository.findAllUserIds(),
                superAdminRepository.findAllUserIds(),
                moderatorRepository.findAllUserIds()
        );
    }

    public List<ActivityView> findAllActivities() {
        return activityRepository.findAll()
                .stream()
                .sorted(activityComparator())
                .map(this::toActivityView)
                .toList();
    }

    public List<ActivityView> findAllActivitiesWithFilters(String search, String timing) {
        String normalizedSearch = normalize(search);
        LocalDate today = LocalDate.now();

        return activityRepository.findAll()
                .stream()
                .filter(activity -> matchesActivitySearch(activity, normalizedSearch))
                .filter(activity -> matchesActivityTiming(activity, timing, today))
                .sorted(activityComparator())
                .map(this::toActivityView)
                .toList();
    }

    public ActivityView updateActivityVerification(UUID id, ActivityVerificationStatus status) {
        if (status == null || status == ActivityVerificationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity verification status is invalid");
        }

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!canVerifyActivity()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to verify this activity");
        }

        activity.setVerificationStatus(status);
        return toActivityView(activityRepository.save(activity));
    }

    private ActivityView toActivityView(Activity activity) {
        String creatorName = activity.getCreator() == null ? "Unknown" : activity.getCreator().getName();
        boolean kdgActivity = activityPolicyService.isKdgActivity(activity);
        ActivityVerificationStatus verificationStatus = activity.getVerificationStatus() == null
                ? ActivityVerificationStatus.PENDING
                : activity.getVerificationStatus();

        return new ActivityView(
                activity.getId(),
                activity.getName(),
                activity.getLocation(),
                activity.getDate(),
                activity.getTime(),
                creatorName,
                kdgActivity,
                verificationStatus,
                canVerifyActivity(),
                hasAuthority()
        );
    }

    private boolean matchesActivitySearch(Activity activity, String search) {
        if (search.isBlank()) {
            return true;
        }

        return normalize(activity.getName()).contains(search)
                || normalize(activity.getLocation()).contains(search)
                || (activity.getCreator() != null && normalize(activity.getCreator().getName()).contains(search))
                || normalize(activityPolicyService.isKdgActivity(activity) ? "KdG" : "Community").contains(search)
                || normalize(activity.getVerificationStatus() == null
                        ? ActivityVerificationStatus.PENDING.name()
                        : activity.getVerificationStatus().name()).contains(search);
    }

    private boolean matchesActivityTiming(Activity activity, String timing, LocalDate today) {
        if (timing == null || timing.isBlank()) {
            return true;
        }

        if (activity.getDate() == null) {
            return false;
        }

        return switch (timing) {
            case "UPCOMING" -> !activity.getDate().isBefore(today);
            case "PAST" -> activity.getDate().isBefore(today);
            default -> true;
        };
    }

    private Comparator<Activity> activityComparator() {
        return Comparator.comparing(Activity::getDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(Activity::getTime, Comparator.nullsLast(LocalTime::compareTo))
                .thenComparing(activity -> normalize(activity.getName()));
    }

    private boolean canVerifyActivity() {
        return hasAuthority();
    }

    private boolean hasAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_MODERATOR".equals(grantedAuthority.getAuthority()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private record RoleLookup(Set<UUID> adminIds, Set<UUID> superAdminIds, Set<UUID> moderatorIds) {
    }
}