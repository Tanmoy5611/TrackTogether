package TrackTogether.service;

import TrackTogether.domain.Activity;
import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.service.delijn.DeLijnService;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelGroupRouteSuggestionServiceTest {

    @Mock
    private TravelGroupService travelGroupService;

    @Mock
    private DeLijnService deLijnService;

    @Mock
    private MessageSource messageSource;

    private TravelGroupRouteSuggestionService routeSuggestionService;

    @BeforeEach
    void setUp() {
        routeSuggestionService = new TravelGroupRouteSuggestionService(
                travelGroupService,
                deLijnService,
                messageSource
        );
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> englishMessage(invocation.getArgument(0)));
    }

    @Test
    void routeSuggestionsAreOnlySupportedForPublicTransportGroups() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        TravelGroup group = travelGroup(groupId, TransportMode.BIKE);

        when(travelGroupService.getTravelGroupById(groupId)).thenReturn(group);
        when(deLijnService.isConfigured()).thenReturn(true);

        TravelGroupRouteSuggestionsDto response = routeSuggestionService.getRouteSuggestions(
                groupId, 4, null, null, null, null, null, null, null);

        assertThat(response.isSupported()).isFalse();
        assertThat(response.getOptions()).isEmpty();
        verify(deLijnService, never()).getRouteOptions(
                eq(group.getDepartureLatitude()),
                eq(group.getDepartureLongitude()),
                eq(group.getDepartureLocation()),
                eq(group.getArrivalLatitude()),
                eq(group.getArrivalLongitude()),
                eq(group.getActivity().getLocation()),
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

        TravelGroupRouteSuggestionsDto response = routeSuggestionService.getRouteSuggestions(
                groupId, 4, null, null, null, null, null, null, null);

        assertThat(response.isSupported()).isTrue();
        assertThat(response.getOptions()).isEmpty();
        assertThat(response.getMessage()).contains("coordinates");
        verify(deLijnService, never()).getRouteOptions(
                eq(group.getDepartureLatitude()),
                eq(group.getDepartureLongitude()),
                eq(group.getDepartureLocation()),
                eq(group.getArrivalLatitude()),
                eq(group.getArrivalLongitude()),
                eq(group.getActivity().getLocation()),
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
                group.getActivity().getLocation(),
                group.getDepartureTime(),
                group.getEstimatedArrivalTime()
        )).thenReturn(List.of(laterOption, earlierOption));

        TravelGroupRouteSuggestionsDto response = routeSuggestionService.getRouteSuggestions(
                groupId, 1, null, null, null, null, null, null, null);

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
                group.getActivity().getLocation(),
                requestedDepartureTime,
                group.getEstimatedArrivalTime()
        )).thenReturn(List.of());

        routeSuggestionService.getRouteSuggestions(
                groupId, 4, null, null, null, null, null, null, requestedDepartureTime);

        verify(deLijnService).getRouteOptions(
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                group.getDepartureLocation(),
                group.getArrivalLatitude(),
                group.getArrivalLongitude(),
                group.getActivity().getLocation(),
                requestedDepartureTime,
                group.getEstimatedArrivalTime()
        );
    }

    private static TravelGroup travelGroup(UUID groupId, TransportMode transportMode) {
        Activity activity = new Activity();
        activity.setLocation("Campus");
        activity.setLatitude(51.2030);
        activity.setLongitude(4.4210);

        TravelGroup group = new TravelGroup(4, "Antwerp Central Station", transportMode);
        ReflectionTestUtils.setField(group, "groupId", groupId);
        group.setActivity(activity);
        group.setDepartureLocation("Antwerp Central Station");
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

    private static String englishMessage(String key) {
        return switch (key) {
            case "routeSuggestions.publicTransportOnly" -> "Route suggestions are only available for public transport groups.";
            case "routeSuggestions.coverage" -> "De Lijn covers buses and trams.";
            case "routeSuggestions.addCoordinates" -> "Add departure and activity coordinates to get De Lijn route suggestions.";
            case "routeSuggestions.apiKeyMissing" -> "De Lijn API key is not configured.";
            case "routeSuggestions.loaded" -> "De Lijn bus/tram route suggestions loaded.";
            case "routeSuggestions.liveDeparturesLoaded" -> "Live De Lijn bus/tram departures near the selected start loaded.";
            case "routeSuggestions.scheduledDeparturesLoaded" -> "Scheduled De Lijn bus/tram departures for the selected date and time loaded.";
            case "routeSuggestions.endpointMissing" -> "De Lijn is configured, but no route planner or nearby stops endpoint is configured yet.";
            case "routeSuggestions.noScheduledDepartures" -> "No scheduled De Lijn bus/tram departures were found near the selected start for this date and time.";
            case "routeSuggestions.noDepartures" -> "No De Lijn bus/tram departures were found near the selected start.";
            default -> key;
        };
    }
}