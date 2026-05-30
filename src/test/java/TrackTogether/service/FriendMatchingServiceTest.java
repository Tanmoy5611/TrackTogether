package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.ActivityVerificationStatus;
import TrackTogether.domain.JoinRequest;
import TrackTogether.domain.JoinRequestStatus;
import TrackTogether.domain.Member;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.dto.delijn.DeLijnStopDto;
import TrackTogether.repository.JoinRequestRepository;
import TrackTogether.repository.TravelGroupMemberRepository;
import TrackTogether.repository.TravelGroupRepository;
import TrackTogether.service.delijn.DeLijnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.MessageSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendMatchingServiceTest {

    @Mock
    private TravelGroupRepository travelGroupRepository;

    @Mock
    private TravelGroupMemberRepository travelGroupMemberRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private DeLijnService deLijnService;

    @Mock
    private ActivityPolicyService activityPolicyService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private FriendMatchingService friendMatchingService;

    @BeforeEach
    void setUpMessages() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> englishMessage(invocation.getArgument(0)));
    }

    @Test
    void suggestionsUseFiltersAndExcludeJoinedFullOwnedAndPastGroups() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        currentUser.setPreferredTransportMode(TransportMode.PUBLIC_TRANSPORT);
        currentUser.setDefaultDepartureLocation("Antwerp Central Station");
        currentUser.setDefaultLatitude(51.2172);
        currentUser.setDefaultLongitude(4.4211);

        Member otherMember = member("22222222-2222-2222-2222-222222222222");
        Activity targetActivity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "AI workshop", 2);
        Activity otherActivity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", "Museum visit", 3);
        Activity pastActivity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3", "Past event", -2);

        TravelGroup joinedGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1",
                targetActivity,
                otherMember,
                TransportMode.CARPOOL,
                "Groenplaats",
                51.2194,
                4.4025,
                4,
                LocalDateTime.of(targetActivity.getDate(), LocalTime.of(12, 0))
        );

        TravelGroup bestMatch = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2",
                targetActivity,
                otherMember,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2173,
                4.4210,
                5,
                LocalDateTime.of(targetActivity.getDate(), LocalTime.of(12, 30))
        );

        TravelGroup wrongActivity = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3",
                otherActivity,
                otherMember,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                5,
                LocalDateTime.of(otherActivity.getDate(), LocalTime.of(9, 0))
        );

        TravelGroup fullGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4",
                targetActivity,
                otherMember,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                1,
                LocalDateTime.of(targetActivity.getDate(), LocalTime.of(12, 15))
        );

        TravelGroup ownedGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5",
                targetActivity,
                currentUser,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                5,
                LocalDateTime.of(targetActivity.getDate(), LocalTime.of(12, 15))
        );

        TravelGroup pastGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6",
                pastActivity,
                otherMember,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                5,
                LocalDateTime.of(pastActivity.getDate(), LocalTime.of(12, 15))
        );

        TravelGroupMember membership = new TravelGroupMember();
        membership.setGroup(joinedGroup);
        membership.setMember(currentUser);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityPolicyService.isVisibleTo(any(), any())).thenReturn(true);
        when(travelGroupMemberRepository.findAllByMember(currentUser)).thenReturn(List.of(membership));
        when(travelGroupRepository.findAll()).thenReturn(List.of(
                joinedGroup,
                bestMatch,
                wrongActivity,
                fullGroup,
                ownedGroup,
                pastGroup
        ));
        when(travelGroupMemberRepository.countMembersByGroupIn(anyList())).thenReturn(List.<Object[]>of(
                countRow(bestMatch, 1),
                countRow(fullGroup, 1)
        ));

        List<TravelFriendSuggestionDto> suggestions = friendMatchingService.suggestTravelGroupsForCurrentUser(
                targetActivity.getId(),
                TransportMode.PUBLIC_TRANSPORT
        );

        assertThat(suggestions).hasSize(1);
        TravelFriendSuggestionDto suggestion = suggestions.getFirst();
        assertThat(suggestion.getGroupId()).isEqualTo(bestMatch.getGroupId());
        assertThat(suggestion.getMatchReasons()).contains(
                "Same event",
                "Also going by public transport",
                "Starts near your location",
                "Leaves around the same time"
        );
    }

    @Test
    void suggestionsAreSortedByBestMatchScore() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        currentUser.setPreferredTransportMode(TransportMode.BIKE);
        currentUser.setDefaultLatitude(51.2172);
        currentUser.setDefaultLongitude(4.4211);

        Member otherMember = member("22222222-2222-2222-2222-222222222222");
        Activity joinedActivity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "Campus meetup", 2);
        Activity otherActivity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2", "Evening run", 2);

        TravelGroup joinedGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1",
                joinedActivity,
                otherMember,
                TransportMode.BIKE,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                4,
                LocalDateTime.of(joinedActivity.getDate(), LocalTime.of(9, 0))
        );
        TravelGroup bestMatch = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2",
                joinedActivity,
                otherMember,
                TransportMode.BIKE,
                "Antwerp Central Station",
                51.2173,
                4.4210,
                4,
                LocalDateTime.of(joinedActivity.getDate(), LocalTime.of(9, 20))
        );
        TravelGroup transportLocationTimeMatch = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3",
                otherActivity,
                otherMember,
                TransportMode.BIKE,
                "Antwerp Central Station",
                51.2174,
                4.4212,
                4,
                LocalDateTime.of(otherActivity.getDate(), LocalTime.of(9, 30))
        );
        TravelGroup sameEventOnly = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4",
                joinedActivity,
                otherMember,
                TransportMode.CAR,
                "Brussels Central",
                50.8467,
                4.3572,
                4,
                LocalDateTime.of(joinedActivity.getDate(), LocalTime.of(17, 0))
        );

        TravelGroupMember membership = new TravelGroupMember();
        membership.setGroup(joinedGroup);
        membership.setMember(currentUser);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityPolicyService.isVisibleTo(any(), any())).thenReturn(true);
        when(travelGroupMemberRepository.findAllByMember(currentUser)).thenReturn(List.of(membership));
        when(travelGroupRepository.findAll()).thenReturn(List.of(
                sameEventOnly,
                transportLocationTimeMatch,
                bestMatch
        ));
        when(travelGroupMemberRepository.countMembersByGroupIn(anyList())).thenReturn(List.<Object[]>of(
                countRow(bestMatch, 1),
                countRow(transportLocationTimeMatch, 1),
                countRow(sameEventOnly, 1)
        ));

        List<TravelFriendSuggestionDto> suggestions = friendMatchingService.suggestTravelGroupsForCurrentUser();

        assertThat(suggestions)
                .extracting(TravelFriendSuggestionDto::getGroupId)
                .containsExactly(
                        bestMatch.getGroupId(),
                        transportLocationTimeMatch.getGroupId(),
                        sameEventOnly.getGroupId()
                );
        assertThat(suggestions.getFirst().getMatchReasons()).contains(
                "Same event",
                "Same transport mode",
                "Starts near your location",
                "Leaves around the same time"
        );
        assertThat(suggestions.getFirst().getScore()).isGreaterThan(suggestions.get(1).getScore());
        assertThat(suggestions.get(1).getScore()).isGreaterThan(suggestions.get(2).getScore());
    }

    @Test
    void suggestionsExcludeGroupsWithPendingJoinRequests() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        currentUser.setPreferredTransportMode(TransportMode.CAR);

        Member otherMember = member("22222222-2222-2222-2222-222222222222");
        Activity activity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "Project fair", 2);

        TravelGroup pendingRequestGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1",
                activity,
                otherMember,
                TransportMode.CAR,
                "Antwerp Central Station",
                51.2172,
                4.4211,
                4,
                LocalDateTime.of(activity.getDate(), LocalTime.of(12, 0))
        );

        TravelGroup availableGroup = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2",
                activity,
                otherMember,
                TransportMode.CAR,
                "Berchem Station",
                51.1992,
                4.4328,
                4,
                LocalDateTime.of(activity.getDate(), LocalTime.of(12, 15))
        );

        JoinRequest pendingRequest = new JoinRequest();
        pendingRequest.setGroup(pendingRequestGroup);
        pendingRequest.setMember(currentUser);
        pendingRequest.setStatus(JoinRequestStatus.PENDING);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityPolicyService.isVisibleTo(any(), any())).thenReturn(true);
        when(travelGroupMemberRepository.findAllByMember(currentUser)).thenReturn(List.of());
        when(travelGroupRepository.findAll()).thenReturn(List.of(pendingRequestGroup, availableGroup));
        when(joinRequestRepository.findAllByMemberAndGroupIn(eq(currentUser), anyList()))
                .thenReturn(List.of(pendingRequest));
        when(travelGroupMemberRepository.countMembersByGroupIn(anyList())).thenReturn(List.<Object[]>of(
                countRow(availableGroup, 1)
        ));

        List<TravelFriendSuggestionDto> suggestions = friendMatchingService.suggestTravelGroupsForCurrentUser(
                activity.getId(),
                TransportMode.CAR
        );

        assertThat(suggestions)
                .extracting(TravelFriendSuggestionDto::getGroupId)
                .containsExactly(availableGroup.getGroupId());
    }

    @Test
    void publicTransportSuggestionsUseDeLijnRouteCompatibility() {
        Member currentUser = member("11111111-1111-1111-1111-111111111111");
        currentUser.setPreferredTransportMode(TransportMode.PUBLIC_TRANSPORT);
        currentUser.setDefaultLatitude(51.2172);
        currentUser.setDefaultLongitude(4.4211);

        Member otherMember = member("22222222-2222-2222-2222-222222222222");
        Activity activity = activity("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1", "Train the trainer", 2);
        LocalDateTime departureTime = LocalDateTime.of(activity.getDate(), LocalTime.of(12, 30));
        LocalDateTime arriveBefore = LocalDateTime.of(activity.getDate(), activity.getTime());
        TravelGroup group = group(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2",
                activity,
                otherMember,
                TransportMode.PUBLIC_TRANSPORT,
                "Antwerp Central Station",
                51.2173,
                4.4210,
                5,
                departureTime
        );
        group.setEstimatedArrivalTime(arriveBefore);

        DeLijnStopDto sharedStop = new DeLijnStopDto(
                "2",
                "202485",
                "Kerk",
                "Destelbergen",
                51.05604,
                3.79717,
                100
        );
        DeLijnRouteOptionDto userRoute = new DeLijnRouteOptionDto(
                sharedStop,
                null,
                departureTime.minusMinutes(5),
                arriveBefore.minusMinutes(15),
                List.of("335"),
                true,
                true
        );
        DeLijnRouteOptionDto groupRoute = new DeLijnRouteOptionDto(
                sharedStop,
                null,
                departureTime,
                arriveBefore.minusMinutes(10),
                List.of("335"),
                true,
                true
        );

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(activityPolicyService.isVisibleTo(any(), any())).thenReturn(true);
        when(travelGroupMemberRepository.findAllByMember(currentUser)).thenReturn(List.of());
        when(travelGroupRepository.findAll()).thenReturn(List.of(group));
        when(travelGroupMemberRepository.countMembersByGroupIn(anyList())).thenReturn(List.<Object[]>of(
                countRow(group, 1)
        ));
        when(deLijnService.isConfigured()).thenReturn(true);
        when(deLijnService.getRouteOptions(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                any(),
                any()
        )).thenReturn(List.of(userRoute)).thenReturn(List.of(groupRoute));

        List<TravelFriendSuggestionDto> suggestions = friendMatchingService.suggestTravelGroupsForCurrentUser(
                activity.getId(),
                TransportMode.PUBLIC_TRANSPORT
        );

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().getMatchReasons()).contains(
                "Same De Lijn stop",
                "Compatible bus/tram route",
                "Arrives before event starts",
                "Similar departure time"
        );
    }

    private static Member member(String id) {
        Member member = new Member();
        member.setUserId(UUID.fromString(id));
        member.setName("Member " + id.substring(0, 4));
        return member;
    }

    private static Activity activity(String id, String name, int daysFromToday) {
        Activity activity = new Activity();
        ReflectionTestUtils.setField(activity, "id", UUID.fromString(id));
        activity.setName(name);
        activity.setDate(LocalDate.now().plusDays(daysFromToday));
        activity.setTime(LocalTime.of(13, 0));
        activity.setLatitude(51.2194);
        activity.setLongitude(4.4025);
        activity.setVerificationStatus(ActivityVerificationStatus.APPROVED);
        return activity;
    }

    private static TravelGroup group(String id,
                                     Activity activity,
                                     Member owner,
                                     TransportMode transportMode,
                                     String departureLocation,
                                     Double latitude,
                                     Double longitude,
                                     int maxMembers,
                                     LocalDateTime departureTime) {
        TravelGroup group = new TravelGroup(maxMembers, departureLocation, transportMode);
        ReflectionTestUtils.setField(group, "groupId", UUID.fromString(id));
        group.setActivity(activity);
        group.setOwner(owner);
        group.setDepartureLocation(departureLocation);
        group.setDepartureLatitude(latitude);
        group.setDepartureLongitude(longitude);
        group.setDepartureTime(departureTime);
        return group;
    }

    private static Object[] countRow(TravelGroup group, long count) {
        return new Object[]{group.getGroupId(), count};
    }

    private static String englishMessage(String key) {
        return switch (key) {
            case "matching.reason.sameEvent" -> "Same event";
            case "matching.reason.sameTransport" -> "Same transport mode";
            case "matching.reason.publicTransport" -> "Also going by public transport";
            case "matching.reason.compatibleTransport" -> "Compatible transport mode";
            case "matching.reason.nearLocation" -> "Starts near your location";
            case "matching.reason.closeLocation" -> "Starts close to your location";
            case "matching.reason.defaultDeparture" -> "Starts from your default departure location";
            case "matching.reason.sameTime" -> "Leaves around the same time";
            case "matching.reason.sameDelijnStop" -> "Same De Lijn stop";
            case "matching.reason.compatibleDelijnRoute" -> "Compatible bus/tram route";
            case "matching.reason.arrivesBeforeEvent" -> "Arrives before event starts";
            case "matching.reason.similarDeparture" -> "Similar departure time";
            case "matching.reason.availableSeats" -> "Available seats";
            default -> key;
        };
    }
}