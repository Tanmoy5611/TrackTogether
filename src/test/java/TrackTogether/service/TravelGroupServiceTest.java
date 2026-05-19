package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.Conversation;
import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.repository.ActivityRepository;
import TrackTogether.repository.ConversationRepository;
import TrackTogether.repository.JoinRequestRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelGroupServiceTest {

    @Mock
    private TravelGroupRepository travelGroupRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private TravelGroupMemberRepository travelGroupMemberRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SystemSettingsService systemSettingsService;

    @InjectMocks
    private TravelGroupService travelGroupService;

    @Test
    void createTravelGroupStoresRouteFieldsAndActivityDestination() {
        UUID activityId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", activityId);
        activity.setDate(LocalDate.now().plusDays(5));
        activity.setTime(LocalTime.of(18, 30));
        activity.setLatitude(51.2030);
        activity.setLongitude(4.4210);

        Member owner = new Member();
        owner.setUserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        LocalDateTime departureTime = LocalDateTime.of(activity.getDate(), LocalTime.of(17, 30));
        LocalDateTime estimatedArrivalTime = LocalDateTime.of(activity.getDate(), LocalTime.of(18, 10));

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(travelGroupRepository.save(any(TravelGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(travelGroupMemberRepository.save(any(TravelGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelGroup group = travelGroupService.createTravelGroup(
                activityId,
                4,
                null,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                TransportMode.PUBLIC_TRANSPORT,
                departureTime,
                estimatedArrivalTime
        );

        assertThat(group.getLocation()).isEqualTo("Antwerp Central Station");
        assertThat(group.getDepartureLocation()).isEqualTo("Antwerp Central Station");
        assertThat(group.getDepartureLatitude()).isEqualTo(51.2172);
        assertThat(group.getDepartureLongitude()).isEqualTo(4.4211);
        assertThat(group.getDepartureTime()).isEqualTo(departureTime);
        assertThat(group.getEstimatedArrivalTime()).isEqualTo(estimatedArrivalTime);
        assertThat(group.getArrivalLatitude()).isEqualTo(activity.getLatitude());
        assertThat(group.getArrivalLongitude()).isEqualTo(activity.getLongitude());
        assertThat(group.getOwner()).isEqualTo(owner);
    }

    @Test
    void joinTravelGroupDirectlyUsesLockedGroupBeforeAddingMember() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        Member owner = member("11111111-1111-1111-1111-111111111111");
        Member joiningMember = member("22222222-2222-2222-2222-222222222222");
        TravelGroup group = travelGroup(groupId, owner, 2);

        when(systemSettingsService.isTravelGroupJoinApprovalEnabled()).thenReturn(false);
        // Simulates the database row lock used in production before checking seats.
        when(travelGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(currentUserService.getCurrentUser()).thenReturn(joiningMember);
        when(travelGroupMemberRepository.existsByGroupAndMember(group, joiningMember)).thenReturn(false);
        // One owner is already in a two-seat group, so the joining student can take the last seat.
        when(travelGroupMemberRepository.countByGroup(group)).thenReturn(1L);
        when(travelGroupMemberRepository.save(any(TravelGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(joinRequestRepository.findByGroupAndMember(group, joiningMember)).thenReturn(Optional.empty());

        travelGroupService.joinTravelGroup(groupId);

        ArgumentCaptor<TravelGroupMember> membershipCaptor = ArgumentCaptor.forClass(TravelGroupMember.class);
        // The important part for the same-time joining issue: the service must use the locked lookup.
        verify(travelGroupRepository).findByIdForUpdate(groupId);
        verify(travelGroupMemberRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getGroup()).isEqualTo(group);
        assertThat(membershipCaptor.getValue().getMember()).isEqualTo(joiningMember);
    }

    @Test
    void joinTravelGroupDirectlyRejectsFullLockedGroup() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
        Member owner = member("11111111-1111-1111-1111-111111111111");
        Member joiningMember = member("22222222-2222-2222-2222-222222222222");
        TravelGroup group = travelGroup(groupId, owner, 1);

        when(systemSettingsService.isTravelGroupJoinApprovalEnabled()).thenReturn(false);
        // Even in the "full group" case, the service should check capacity through the locked group.
        when(travelGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(currentUserService.getCurrentUser()).thenReturn(joiningMember);
        when(travelGroupMemberRepository.existsByGroupAndMember(group, joiningMember)).thenReturn(false);
        // The owner already fills the only seat, so no new membership may be saved.
        when(travelGroupMemberRepository.countByGroup(group)).thenReturn(1L);

        assertThatThrownBy(() -> travelGroupService.joinTravelGroup(groupId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("Travel group is full");
                });

        // This proves a full locked group fails cleanly instead of overbooking the last seat.
        verify(travelGroupRepository).findByIdForUpdate(groupId);
        verify(travelGroupMemberRepository, never()).save(any(TravelGroupMember.class));
    }

    @Test
    void acceptJoinRequestUsesLockedGroupBeforeAddingMember() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");
        Member owner = member("11111111-1111-1111-1111-111111111111");
        Member joiningMember = member("22222222-2222-2222-2222-222222222222");
        TravelGroup group = travelGroup(groupId, owner, 2);
        JoinRequest joinRequest = joinRequest(group, joiningMember);

        when(joinRequestRepository.findById(10)).thenReturn(Optional.of(joinRequest));
        // Accepting a request also locks the group, because it inserts a member and uses capacity.
        when(travelGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(travelGroupMemberRepository.existsByGroupAndMember(group, joiningMember)).thenReturn(false);
        // One free seat remains, so the owner can safely accept the request.
        when(travelGroupMemberRepository.countByGroup(group)).thenReturn(1L);
        when(travelGroupMemberRepository.save(any(TravelGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        travelGroupService.acceptJoinRequest(10);

        ArgumentCaptor<TravelGroupMember> membershipCaptor = ArgumentCaptor.forClass(TravelGroupMember.class);
        // Verifies the acceptance path uses the same lock-based protection as direct joining.
        verify(travelGroupRepository).findByIdForUpdate(groupId);
        verify(travelGroupMemberRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getGroup()).isEqualTo(group);
        assertThat(membershipCaptor.getValue().getMember()).isEqualTo(joiningMember);
    }

    @Test
    void acceptJoinRequestRejectsFullLockedGroup() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4");
        Member owner = member("11111111-1111-1111-1111-111111111111");
        Member joiningMember = member("22222222-2222-2222-2222-222222222222");
        TravelGroup group = travelGroup(groupId, owner, 1);
        JoinRequest joinRequest = joinRequest(group, joiningMember);

        when(joinRequestRepository.findById(10)).thenReturn(Optional.of(joinRequest));
        // The owner can click accept at the same time as another join action, so this path must lock too.
        when(travelGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(travelGroupMemberRepository.existsByGroupAndMember(group, joiningMember)).thenReturn(false);
        // The group is already full when the locked capacity check runs.
        when(travelGroupMemberRepository.countByGroup(group)).thenReturn(1L);

        assertThatThrownBy(() -> travelGroupService.acceptJoinRequest(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("Travel group is full");
                });

        // No membership is inserted when the locked capacity check says the group is full.
        verify(travelGroupRepository).findByIdForUpdate(groupId);
        verify(travelGroupMemberRepository, never()).save(any(TravelGroupMember.class));
    }

    private static Member member(String userId) {
        Member member = new Member();
        member.setUserId(UUID.fromString(userId));
        return member;
    }

    private static TravelGroup travelGroup(UUID groupId, Member owner, int maxMembers) {
        TravelGroup group = new TravelGroup(maxMembers, "Antwerp Central Station", TransportMode.PUBLIC_TRANSPORT);
        ReflectionTestUtils.setField(group, "groupId", groupId);
        group.setOwner(owner);
        return group;
    }

    private static JoinRequest joinRequest(TravelGroup group, Member member) {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setGroup(group);
        joinRequest.setMember(member);
        return joinRequest;
    }
}