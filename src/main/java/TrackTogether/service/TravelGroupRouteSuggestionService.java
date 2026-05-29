package TrackTogether.service;

import TrackTogether.domain.TransportMode;
import TrackTogether.domain.TravelGroup;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.service.delijn.DeLijnService;
import TrackTogether.webapi.dto.TravelGroupRouteSuggestionsDto;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TravelGroupRouteSuggestionService {

    private final TravelGroupService travelGroupService;
    private final DeLijnService deLijnService;
    private final MessageSource messageSource;

    // Wires group lookup with De Lijn routing and localized messages
    public TravelGroupRouteSuggestionService(TravelGroupService travelGroupService,
                                             DeLijnService deLijnService,
                                             MessageSource messageSource) {
        this.travelGroupService = travelGroupService;
        this.deLijnService = deLijnService;
        this.messageSource = messageSource;
    }

    // Builds De Lijn route suggestions for a travel group and optional user-selected route points
    public TravelGroupRouteSuggestionsDto getRouteSuggestions(UUID groupId,
                                                              int maxResults,
                                                              Double originLatitude,
                                                              Double originLongitude,
                                                              String originLabel,
                                                              Double destinationLatitude,
                                                              Double destinationLongitude,
                                                              String destinationLabel,
                                                              LocalDateTime departureTime) {
        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        TravelGroup group = travelGroupService.getTravelGroupById(groupId);

        // User-selected coordinates override saved group route points
        Double arrivalLatitude = group.getArrivalLatitude() != null
                ? group.getArrivalLatitude()
                : group.getActivity().getLatitude();
        Double arrivalLongitude = group.getArrivalLongitude() != null
                ? group.getArrivalLongitude()
                : group.getActivity().getLongitude();
        Double routeOriginLatitude = originLatitude != null ? originLatitude : group.getDepartureLatitude();
        Double routeOriginLongitude = originLongitude != null ? originLongitude : group.getDepartureLongitude();
        Double routeDestinationLatitude = destinationLatitude != null ? destinationLatitude : arrivalLatitude;
        Double routeDestinationLongitude = destinationLongitude != null ? destinationLongitude : arrivalLongitude;
        String routeOriginLabel = hasText(originLabel) ? originLabel : group.getDepartureLocation();
        String routeDestinationLabel = hasText(destinationLabel) ? destinationLabel : group.getActivity().getLocation();
        LocalDateTime routeDepartureTime = departureTime != null ? departureTime : group.getDepartureTime();

        // Only public transport groups can use De Lijn route suggestions
        if (group.getTransportMode() != TransportMode.PUBLIC_TRANSPORT) {
            return new TravelGroupRouteSuggestionsDto(
                    false,
                    deLijnService.isConfigured(),
                    message("routeSuggestions.publicTransportOnly"),
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    message("routeSuggestions.coverage"),
                    List.of()
            );
        }

        if (!hasCoordinates(routeOriginLatitude, routeOriginLongitude)
                || !hasCoordinates(routeDestinationLatitude, routeDestinationLongitude)) {
            return new TravelGroupRouteSuggestionsDto(
                    true,
                    deLijnService.isConfigured(),
                    message("routeSuggestions.addCoordinates"),
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    message("routeSuggestions.coverage"),
                    List.of()
            );
        }

        if (!deLijnService.isConfigured()) {
            return new TravelGroupRouteSuggestionsDto(
                    true,
                    false,
                    message("routeSuggestions.apiKeyMissing"),
                    routeOriginLabel,
                    routeOriginLatitude,
                    routeOriginLongitude,
                    routeDestinationLabel,
                    routeDestinationLatitude,
                    routeDestinationLongitude,
                    message("routeSuggestions.coverage"),
                    List.of()
            );
        }

        List<DeLijnRouteOptionDto> options = deLijnService.getRouteOptions(
                        routeOriginLatitude,
                        routeOriginLongitude,
                        routeOriginLabel,
                        routeDestinationLatitude,
                        routeDestinationLongitude,
                        routeDestinationLabel,
                        routeDepartureTime,
                        group.getEstimatedArrivalTime()
                )
                .stream()
                // Sort before balancing so the final list keeps the earliest useful departures
                .sorted(Comparator.comparing(
                        DeLijnRouteOptionDto::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        return new TravelGroupRouteSuggestionsDto(
                true,
                true,
                routeSuggestionMessage(options),
                routeOriginLabel,
                routeOriginLatitude,
                routeOriginLongitude,
                routeDestinationLabel,
                routeDestinationLatitude,
                routeDestinationLongitude,
                message("routeSuggestions.coverage"),
                balancedTransitOptions(options, maxResults)
        );
    }

    // Validates complete latitude and longitude pairs before external route calls
    private static boolean hasCoordinates(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    // Checks whether a label from the request can override the saved label
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Chooses the status message that best matches the De Lijn integration path
    private String routeSuggestionMessage(List<DeLijnRouteOptionDto> options) {
        if (!options.isEmpty()) {
            if (deLijnService.hasRouteOptionsEndpoint()) {
                return message("routeSuggestions.loaded");
            }

            boolean realtimeOnly = options.stream().allMatch(DeLijnRouteOptionDto::isRealtime);
            return realtimeOnly
                    ? message("routeSuggestions.liveDeparturesLoaded")
                    : message("routeSuggestions.scheduledDeparturesLoaded");
        }

        if (!deLijnService.hasRouteOptionsEndpoint() && !deLijnService.hasNearbyStopsEndpoint()) {
            return message("routeSuggestions.endpointMissing");
        }

        if (deLijnService.hasScheduledDeparturesEndpoint()) {
            return message("routeSuggestions.noScheduledDepartures");
        }

        return message("routeSuggestions.noDepartures");
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    // Keeps bus and tram options visible when both are available
    private static List<DeLijnRouteOptionDto> balancedTransitOptions(List<DeLijnRouteOptionDto> options, int maxResults) {
        if (options.size() <= maxResults) {
            return options;
        }

        Set<DeLijnRouteOptionDto> selected = new LinkedHashSet<>();
        addFirstTransportType(options, selected, "BUS");
        addFirstTransportType(options, selected, "TRAM");

        for (DeLijnRouteOptionDto option : options) {
            if (selected.size() >= maxResults) {
                break;
            }
            selected.add(option);
        }

        return new ArrayList<>(selected).stream()
                .sorted(Comparator.comparing(
                        DeLijnRouteOptionDto::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    // Seeds the balanced result with the earliest option for one transport type
    private static void addFirstTransportType(List<DeLijnRouteOptionDto> options,
                                              Set<DeLijnRouteOptionDto> selected,
                                              String transportType) {
        options.stream()
                .filter(option -> transportType.equalsIgnoreCase(option.getTransportType()))
                .findFirst()
                .ifPresent(selected::add);
    }
}