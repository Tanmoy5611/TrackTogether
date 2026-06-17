package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import TrackTogether.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ActivityPolicyService activityPolicyService;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(
                activityRepository,
                mock(TravelGroupService.class),
                currentUserService,
                activityPolicyService
        );
    }

    @Test
    void returnsApprovedActivitiesAndCurrentUsersPendingActivities() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        Activity approved = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", ActivityVerificationStatus.APPROVED, null);
        Activity ownPending = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", ActivityVerificationStatus.PENDING, currentUser);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityRepository.findAllByVerificationStatusOrCreator(
                ActivityVerificationStatus.APPROVED,
                currentUser
        )).thenReturn(List.of(approved, ownPending));

        assertThat(activityService.getAllActivities()).containsExactly(approved, ownPending);
    }

    @Test
    void blocksPendingActivityDetailsForOtherUsers() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        Member creator = member("22222222-2222-2222-2222-222222222222");
        Activity pending = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", ActivityVerificationStatus.PENDING, creator);

        when(activityRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityPolicyService.isVisibleTo(pending, currentUser)).thenReturn(false);

        assertThatThrownBy(() -> activityService.getActivityById(pending.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void keepsNewActivitiesPendingBeforeModeratorReview() {
        Member creator = member("11111111-1111-1111-1111-111111111111");
        Activity activity = new Activity();
        activity.setVerificationStatus(ActivityVerificationStatus.APPROVED);

        when(currentUserService.getCurrentUser()).thenReturn(creator);
        when(activityRepository.save(activity)).thenReturn(activity);

        Activity saved = activityService.createActivity(activity);

        assertThat(saved.getCreator()).isEqualTo(creator);
        assertThat(saved.getVerificationStatus()).isEqualTo(ActivityVerificationStatus.PENDING);
    }

    @Test
    void marksActivitiesCreatedByStaffAsKdgActivities() {
        Member creator = member("11111111-1111-1111-1111-111111111111");
        Activity activity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", ActivityVerificationStatus.APPROVED, creator);

        when(activityPolicyService.isKdgActivity(activity)).thenReturn(true);

        assertThat(activityService.isKdgActivity(activity)).isTrue();
        assertThat(activityService.getKdgActivityIds(List.of(activity))).containsExactly(activity.getId());
    }

    private static Member member(String id) {
        Member member = new Member();
        member.setUserId(UUID.fromString(id));
        return member;
    }

    private static Activity activity(String id, ActivityVerificationStatus status, Member creator) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", UUID.fromString(id));
        activity.setVerificationStatus(status);
        activity.setCreator(creator);
        return activity;
    }
}