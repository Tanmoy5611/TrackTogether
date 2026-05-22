package TrackTogether.webapi;

import TrackTogether.dto.delijn.DeLijnDebugDto;
import TrackTogether.dto.delijn.DeLijnDepartureDto;
import TrackTogether.dto.delijn.DeLijnStopDto;
import TrackTogether.service.delijn.DeLijnService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/delijn")
public class DeLijnApiController {

    private final DeLijnService deLijnService;

    // Wires De Lijn API endpoints to the service that calls and parses De Lijn data
    public DeLijnApiController(DeLijnService deLijnService) {
        this.deLijnService = deLijnService;
    }

    // Returns De Lijn stops near a coordinate.
    @GetMapping("/nearby-stops")
    public List<DeLijnStopDto> findNearbyStops(@RequestParam("lat") Double latitude,
                                               @RequestParam("lng") Double longitude,
                                               @RequestParam(defaultValue = "1000") int radiusMeters,
                                               @RequestParam(defaultValue = "5") int maxResults) {
        validateCoordinates(latitude, longitude);

        if (radiusMeters < 1 || radiusMeters > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be between 1 and 5000 meters");
        }

        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        ensureConfigured();

        return deLijnService.findNearbyStops(latitude, longitude, radiusMeters, maxResults);
    }

    // Returns raw diagnostics for nearby stop lookup
    @GetMapping("/nearby-stops/debug")
    public DeLijnDebugDto debugNearbyStops(@RequestParam("lat") Double latitude,
                                           @RequestParam("lng") Double longitude,
                                           @RequestParam(defaultValue = "1000") int radiusMeters) {
        validateCoordinates(latitude, longitude);

        if (radiusMeters < 1 || radiusMeters > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be between 1 and 5000 meters");
        }

        return deLijnService.debugNearbyStops(latitude, longitude, radiusMeters);
    }

    // Provides stop suggestions for route planner text inputs
    @GetMapping("/stop-suggestions")
    public List<DeLijnStopDto> suggestStops(@RequestParam("q") String query,
                                            @RequestParam(required = false) Double lat,
                                            @RequestParam(required = false) Double lng,
                                            @RequestParam(defaultValue = "15") int maxResults) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        if (lat != null || lng != null) {
            validateCoordinates(lat, lng);
        }

        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        ensureConfigured();

        return deLijnService.searchStops(query.trim(), lat, lng, maxResults);
    }

    // Returns scheduled departures for one stop and travel date
    @GetMapping("/stops/{entityNumber}/{stopNumber}/scheduled-departures")
    public List<DeLijnDepartureDto> getScheduledDepartures(@PathVariable String entityNumber,
                                                           @PathVariable String stopNumber,
                                                           @RequestParam java.time.LocalDate date,
                                                           @RequestParam(required = false) java.time.LocalDateTime departureTime,
                                                           @RequestParam(defaultValue = "8") int maxResults) {
        ensureConfigured();

        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        return deLijnService.getScheduledDepartures(entityNumber, stopNumber, date, departureTime, maxResults);
    }

    // Returns details for one De Lijn stop
    @GetMapping("/stops/{entityNumber}/{stopNumber}")
    public DeLijnStopDto getStopDetails(@PathVariable String entityNumber,
                                        @PathVariable String stopNumber) {
        ensureConfigured();

        return deLijnService.getStopDetails(entityNumber, stopNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "De Lijn stop not found"));
    }

    // Returns raw diagnostics for the stop details endpoint.
    @GetMapping("/stops/{entityNumber}/{stopNumber}/debug")
    public DeLijnDebugDto debugStopDetails(@PathVariable String entityNumber,
                                           @PathVariable String stopNumber) {
        return deLijnService.debugStopDetails(entityNumber, stopNumber);
    }

    // Probes possible De Lijn endpoint variants for a stop.
    @GetMapping("/stops/{entityNumber}/{stopNumber}/probe")
    public List<DeLijnDebugDto> probeCoreEndpoints(@PathVariable String entityNumber,
                                                   @PathVariable String stopNumber,
                                                   @RequestParam(defaultValue = "4") int maxResults) {
        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        return deLijnService.probeCoreEndpoints(entityNumber, stopNumber, maxResults);
    }

    // Returns live departures for one stop
    @GetMapping("/stops/{entityNumber}/{stopNumber}/departures")
    public List<DeLijnDepartureDto> getRealtimeDepartures(@PathVariable String entityNumber,
                                                          @PathVariable String stopNumber,
                                                          @RequestParam(defaultValue = "4") int maxResults) {
        ensureConfigured();

        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        return deLijnService.getRealtimeDepartures(entityNumber, stopNumber, maxResults);
    }

    // Returns raw diagnostics for live departures
    @GetMapping("/stops/{entityNumber}/{stopNumber}/departures/debug")
    public DeLijnDebugDto debugRealtimeDepartures(@PathVariable String entityNumber,
                                                  @PathVariable String stopNumber,
                                                  @RequestParam(defaultValue = "4") int maxResults) {
        if (maxResults < 1 || maxResults > 25) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max results must be between 1 and 25");
        }

        return deLijnService.debugRealtimeDepartures(entityNumber, stopNumber, maxResults);
    }

    // Validates latitude and longitude request parameters
    private static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90");
        }

        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180");
        }
    }

    // Rejects De Lijn API calls when the API key is missing
    private void ensureConfigured() {
        if (!deLijnService.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "De Lijn API key is not configured. Set DELIJN_API_KEY and restart the app."
            );
        }
    }
}