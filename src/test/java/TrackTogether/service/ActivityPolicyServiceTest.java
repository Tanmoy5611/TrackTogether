package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.Member;
import TrackTogether.repository.AdminRepository;
import TrackTogether.repository.ModeratorRepository;
import TrackTogether.repository.SuperAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityPolicyServiceTest {

    @Mock
    private ModeratorRepository moderatorRepository;

    private ActivityPolicyService activityPolicyService;

    @BeforeEach
    void setUp() {
        activityPolicyService = new ActivityPolicyService(
                mock(AdminRepository.class),
                mock(SuperAdminRepository.class),
                moderatorRepository
        );
    }

    @Test
    void approvedActivitiesAreVisibleToEveryone() {
        Member user = member("11111111-1111-1111-1111-111111111111");
        Activity activity = activity(ActivityVerificationStatus.APPROVED, null);

        assertThat(activityPolicyService.isVisibleTo(activity, user)).isTrue();
    }

    @Test
    void pendingActivitiesAreVisibleOnlyToTheirCreator() {
        Member creator = member("11111111-1111-1111-1111-111111111111");
        Member otherUser = member("22222222-2222-2222-2222-222222222222");
        Activity activity = activity(ActivityVerificationStatus.PENDING, creator);

        assertThat(activityPolicyService.isVisibleTo(activity, creator)).isTrue();
        assertThat(activityPolicyService.isVisibleTo(activity, otherUser)).isFalse();
    }

    @Test
    void staffCreatedActivitiesAreKdgActivities() {
        Member creator = member("11111111-1111-1111-1111-111111111111");
        Activity activity = activity(ActivityVerificationStatus.APPROVED, creator);

        when(moderatorRepository.existsByUserId(creator.getUserId())).thenReturn(true);

        assertThat(activityPolicyService.isKdgActivity(activity)).isTrue();
    }

    private static Activity activity(ActivityVerificationStatus status, Member creator) {
        Activity activity = new Activity();
        activity.setVerificationStatus(status);
        activity.setCreator(creator);
        return activity;
    }

    private static Member member(String id) {
        Member member = new Member();
        member.setUserId(UUID.fromString(id));
        return member;
    }
}