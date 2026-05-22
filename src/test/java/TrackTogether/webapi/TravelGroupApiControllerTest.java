package TrackTogether.webapi;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.service.FriendMatchingService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.service.delijn.DeLijnService;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TravelGroupApiControllerTest {

    @Mock
    private TravelGroupService travelGroupService;

    @Mock
    private TravelGroupMapper travelGroupMapper;

    @Mock
    private FriendMatchingService friendMatchingService;

    @Mock
    private DeLijnService deLijnService;

    private TravelGroupApiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TravelGroupApiController(
                travelGroupService,
                travelGroupMapper,
                friendMatchingService,
                deLijnService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void suggestionsEndpointUsesFiltersAndReturnsSuggestionCards() throws Exception {
        UUID activityId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        TravelFriendSuggestionDto suggestion = new TravelFriendSuggestionDto(
                groupId,
                activityId,
                "AI workshop",
                "Antwerp Central Station",
                LocalDateTime.of(2026, 6, 10, 12, 30),
                TransportMode.PUBLIC_TRANSPORT,
                2,
                5,
                140,
                List.of("Same event", "Also going by public transport")
        );

        when(friendMatchingService.suggestTravelGroupsForCurrentUser(activityId, TransportMode.PUBLIC_TRANSPORT))
                .thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/travelgroups/suggestions")
                        .param("activityId", activityId.toString())
                        .param("transportMode", "PUBLIC_TRANSPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(groupId.toString()))
                .andExpect(jsonPath("$[0].activityId").value(activityId.toString()))
                .andExpect(jsonPath("$[0].activityName").value("AI workshop"))
                .andExpect(jsonPath("$[0].departureLocation").value("Antwerp Central Station"))
                .andExpect(jsonPath("$[0].transportMode").value("PUBLIC_TRANSPORT"))
                .andExpect(jsonPath("$[0].currentMemberCount").value(2))
                .andExpect(jsonPath("$[0].maxMembers").value(5))
                .andExpect(jsonPath("$[0].availableSpots").value(3))
                .andExpect(jsonPath("$[0].score").value(140))
                .andExpect(jsonPath("$[0].matchReasons[0]").value("Same event"))
                .andExpect(jsonPath("$[0].matchReasons[1]").value("Also going by public transport"));

        verify(friendMatchingService).suggestTravelGroupsForCurrentUser(activityId, TransportMode.PUBLIC_TRANSPORT);
    }

    @Test
    void routeSuggestionsAreOnlySupportedForPublicTransportGroups() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        TravelGroup group = travelGroup(groupId, TransportMode.BIKE);

        when(travelGroupService.getTravelGroupById(groupId)).thenReturn(group);
        when(deLijnService.isConfigured()).thenReturn(true);

        TravelGroupRouteSuggestionsDto response = controller.getRouteSuggestions(groupId, 4, null, null, null, null, null, null, null);

        assertThat(response.isSupported()).isFalse();
        assertThat(response.getOptions()).isEmpty();
        verify(deLijnService, never()).getRouteOptions(
                eq(group.getDepartureLatitude()),
                eq(group.getDepartureLongitude()),
                eq(group.getDepartureLocation()),
                eq(group.getArrivalLatitude()),
                eq(group.getArrivalLongitude()),
                eq(null),
                eq(group.getDepartureTime()),
                eq(group.getEstimatedArrivalTime())
        );
    }

    @Test
    void routeSuggestionsRequireDepartureAndArrivalCoordinates() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
        TravelGroup group = travelGroup(groupId, TransportMode.PUBLIC_TRANSPORT);
        group.setDepartureLatitude(null);

        when(travelGroupService.getTravelGroupById(groupId)).thenReturn(group);
        when(deLijnService.isConfigured()).thenReturn(true);

        TravelGroupRouteSuggestionsDto response = controller.getRouteSuggestions(groupId, 4, null, null, null, null, null, null, null);

        assertThat(response.isSupported()).isTrue();
        assertThat(response.getOptions()).isEmpty();
        assertThat(response.getMessage()).contains("coordinates");
        verify(deLijnService, never()).getRouteOptions(
                eq(group.getDepartureLatitude()),
                eq(group.getDepartureLongitude()),
                eq(group.getDepartureLocation()),
                eq(group.getArrivalLatitude()),
                eq(group.getArrivalLongitude()),
                eq(null),
                eq(group.getDepartureTime()),
                eq(group.getEstimatedArrivalTime())
        );
    }

    @Test
    void routeSuggestionsAreSortedAndLimited() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");
        TravelGroup group = travelGroup(groupId, TransportMode.PUBLIC_TRANSPORT);
        DeLijnRouteOptionDto laterOption = routeOption(LocalDateTime.of(2026, 5, 20, 18, 20));
        DeLijnRouteOptionDto earlierOption = routeOption(LocalDateTime.of(2026, 5, 20, 18, 5));

        when(travelGroupService.getTravelGroupById(groupId)).thenReturn(group);
        when(deLijnService.isConfigured()).thenReturn(true);
        when(deLijnService.getRouteOptions(
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                group.getDepartureLocation(),
                group.getArrivalLatitude(),
                group.getArrivalLongitude(),
                null,
                group.getDepartureTime(),
                group.getEstimatedArrivalTime()
        )).thenReturn(List.of(laterOption, earlierOption));

        TravelGroupRouteSuggestionsDto response = controller.getRouteSuggestions(groupId, 1, null, null, null, null, null, null, null);

        assertThat(response.isSupported()).isTrue();
        assertThat(response.isConfigured()).isTrue();
        assertThat(response.getOptions()).containsExactly(earlierOption);
    }

    @Test
    void routeSuggestionsUseRequestedDepartureTimeWhenProvided() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4");
        TravelGroup group = travelGroup(groupId, TransportMode.PUBLIC_TRANSPORT);
        LocalDateTime requestedDepartureTime = LocalDateTime.of(2026, 5, 21, 14, 30);

        when(travelGroupService.getTravelGroupById(groupId)).thenReturn(group);
        when(deLijnService.isConfigured()).thenReturn(true);
        when(deLijnService.getRouteOptions(
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                group.getDepartureLocation(),
                group.getArrivalLatitude(),
                group.getArrivalLongitude(),
                null,
                requestedDepartureTime,
                group.getEstimatedArrivalTime()
        )).thenReturn(List.of());

        controller.getRouteSuggestions(groupId, 4, null, null, null, null, null, null, requestedDepartureTime);

        verify(deLijnService).getRouteOptions(
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                group.getDepartureLocation(),
                group.getArrivalLatitude(),
                group.getArrivalLongitude(),
                null,
                requestedDepartureTime,
                group.getEstimatedArrivalTime()
        );
    }

    private static TravelGroup travelGroup(UUID groupId, TransportMode transportMode) {
        Activity activity = new Activity();
        activity.setLatitude(51.2030);
        activity.setLongitude(4.4210);

        TravelGroup group = new TravelGroup(4, "Antwerp Central Station", transportMode);
        ReflectionTestUtils.setField(group, "groupId", groupId);
        group.setActivity(activity);
        group.setDepartureLatitude(51.2172);
        group.setDepartureLongitude(4.4211);
        group.setArrivalLatitude(activity.getLatitude());
        group.setArrivalLongitude(activity.getLongitude());
        group.setDepartureTime(LocalDateTime.of(2026, 5, 20, 18, 0));
        group.setEstimatedArrivalTime(LocalDateTime.of(2026, 5, 20, 18, 45));
        return group;
    }

    private static DeLijnRouteOptionDto routeOption(LocalDateTime departureTime) {
        return new DeLijnRouteOptionDto(
                null,
                null,
                departureTime,
                departureTime.plusMinutes(25),
                List.of("1"),
                true,
                true
        );
    }
}