package TrackTogether.webapi;

import TrackTogether.domain.TransportMode;
import TrackTogether.dto.TravelFriendSuggestionDto;
import TrackTogether.service.FriendMatchingService;
import TrackTogether.service.TravelGroupRouteSuggestionService;
import TrackTogether.service.TravelGroupService;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import TrackTogether.webapi.mapper.TravelGroupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
    private TravelGroupRouteSuggestionService routeSuggestionService;

    @Mock
    private MessageSource messageSource;

    private TravelGroupApiController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TravelGroupApiController(
                travelGroupService,
                travelGroupMapper,
                friendMatchingService,
                routeSuggestionService,
                messageSource
        );
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> englishMessage(invocation.getArgument(0)));
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
    void routeSuggestionsEndpointDelegatesToService() {
        UUID groupId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        LocalDateTime requestedDepartureTime = LocalDateTime.of(2026, 5, 21, 14, 30);
        TravelGroupRouteSuggestionsDto routeSuggestions = new TravelGroupRouteSuggestionsDto(
                true,
                true,
                "De Lijn bus/tram route suggestions loaded.",
                "Antwerp Central Station",
                51.2172,
                4.4211,
                "Campus",
                51.2030,
                4.4210,
                "De Lijn covers buses and trams.",
                List.of()
        );

        when(routeSuggestionService.getRouteSuggestions(
                groupId,
                4,
                51.2172,
                4.4211,
                "Antwerp Central Station",
                51.2030,
                4.4210,
                "Campus",
                requestedDepartureTime
        )).thenReturn(routeSuggestions);

        controller.getRouteSuggestions(
                groupId,
                4,
                51.2172,
                4.4211,
                "Antwerp Central Station",
                51.2030,
                4.4210,
                "Campus",
                requestedDepartureTime
        );

        verify(routeSuggestionService).getRouteSuggestions(
                groupId,
                4,
                51.2172,
                4.4211,
                "Antwerp Central Station",
                51.2030,
                4.4210,
                "Campus",
                requestedDepartureTime
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
            case "flash.travelGroup.joinRequestSent" -> "Join request sent. The group owner can accept or reject it.";
            case "html.you.re.going.too" -> "You're going too!";
            default -> key;
        };
    }
}