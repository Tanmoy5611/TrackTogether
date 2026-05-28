package TrackTogether.service.delijn;

import TrackTogether.config.DeLijnProperties;
import TrackTogether.dto.delijn.DeLijnDebugDto;
import TrackTogether.dto.delijn.DeLijnDepartureDto;
import TrackTogether.dto.delijn.DeLijnRouteOptionDto;
import TrackTogether.dto.delijn.DeLijnStopDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

@Service
public class DeLijnService {

    private static final Logger LOGGER = Logger.getLogger(DeLijnService.class.getName());
    private static final String API_KEY_HEADER = "Ocp-Apim-Subscription-Key";
    private static final int DEBUG_BODY_PREVIEW_LENGTH = 1200;
    private static final int MAX_DEPARTURE_STOP_CANDIDATES = 6;
    private static final String TRANSPORT_BUS = "BUS";
    private static final String TRANSPORT_TRAM = "TRAM";
    private static final Set<String> ANTWERP_TRAM_LINES = Set.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "15", "24");
    private static final Set<String> GHENT_TRAM_LINES = Set.of("1", "2", "4");

    private final RestClient restClient;
    private final DeLijnProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Creates the service with the configured De Lijn REST client and endpoint settings
    public DeLijnService(RestClient deLijnRestClient, DeLijnProperties properties) {
        this.restClient = deLijnRestClient;
        this.properties = properties;
    }

    // Checks whether De Lijn integration can make authenticated API calls
    public boolean isConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
    }

    // Indicates whether a real route planner endpoint has been configured
    public boolean hasRouteOptionsEndpoint() {
        return StringUtils.hasText(properties.getEndpoints().getRouteOptions());
    }

    // Indicates whether nearby stops can be fetched directly from De Lijn
    public boolean hasNearbyStopsEndpoint() {
        return StringUtils.hasText(properties.getEndpoints().getNearbyStops());
    }

    // Indicates whether scheduled departures can be fetched for a stop and date
    public boolean hasScheduledDeparturesEndpoint() {
        return StringUtils.hasText(properties.getEndpoints().getScheduledDepartures());
    }

    // Finds nearby stops using the default radius and result limit from configuration
    public List<DeLijnStopDto> findNearbyStops(Double latitude, Double longitude) {
        return findNearbyStops(
                latitude,
                longitude,
                properties.getNearbyStopRadiusMeters(),
                properties.getMaxNearbyStops()
        );
    }

    // Finds nearby De Lijn stops around a coordinate and returns the closest matches first
    public List<DeLijnStopDto> findNearbyStops(Double latitude,
                                               Double longitude,
                                               int radiusMeters,
                                               int maxResults) {
        if (!isConfigured() || latitude == null || longitude == null) {
            return List.of();
        }

        Map<String, Object> variables = Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "radiusMeters", Math.max(radiusMeters, 1)
        );

        // De Lijn can return stops in different payload shapes, so parsing is centralized before sorting by distance
        return fetchJson(properties.getEndpoints().getNearbyStops(), variables)
                .map(this::parseStops)
                .orElseGet(List::of)
                .stream()
                .sorted(Comparator.comparing(
                        DeLijnStopDto::getDistanceMeters,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(Math.max(maxResults, 1))
                .toList();
    }

    // Calls the nearby stops endpoint and returns raw diagnostics for troubleshooting API paths and payloads
    public DeLijnDebugDto debugNearbyStops(Double latitude, Double longitude, int radiusMeters) {
        String path = properties.getEndpoints().getNearbyStops();
        if (!isConfigured()) {
            return new DeLijnDebugDto(
                    false,
                    path,
                    null,
                    0,
                    null,
                    "De Lijn API key is not configured"
            );
        }

        if (!StringUtils.hasText(path)) {
            return new DeLijnDebugDto(
                    true,
                    path,
                    null,
                    0,
                    null,
                    "Nearby stops endpoint is not configured. Check the Search API operation path in the De Lijn portal and set DELIJN_API_NEARBY_STOPS_PATH."
            );
        }

        Map<String, Object> variables = Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "radiusMeters", Math.max(radiusMeters, 1)
        );

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(path, variables)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();
            int parsedStopCount = parseRawStopCount(body);
            return new DeLijnDebugDto(
                    true,
                    path,
                    response.getStatusCode().value(),
                    parsedStopCount,
                    preview(body),
                    null
            );
        } catch (RestClientResponseException exception) {
            return new DeLijnDebugDto(
                    true,
                    path,
                    exception.getStatusCode().value(),
                    0,
                    preview(exception.getResponseBodyAsString()),
                    "De Lijn returned HTTP " + exception.getStatusCode().value()
            );
        } catch (RuntimeException exception) {
            return new DeLijnDebugDto(
                    true,
                    path,
                    null,
                    0,
                    null,
                    exception.getMessage()
            );
        }
    }

    // Gets real-time departures using the configured default maximum
    public List<DeLijnDepartureDto> getRealtimeDepartures(String entityNumber, String stopNumber) {
        return getRealtimeDepartures(entityNumber, stopNumber, properties.getMaxDepartures());
    }

    // Gets real-time departures for a specific De Lijn stop
    public List<DeLijnDepartureDto> getRealtimeDepartures(String entityNumber,
                                                          String stopNumber,
                                                          int maxDepartures) {
        if (!isConfigured() || !StringUtils.hasText(entityNumber) || !StringUtils.hasText(stopNumber)) {
            return List.of();
        }

        Map<String, Object> variables = Map.of(
                "entityNumber", entityNumber,
                "stopNumber", stopNumber,
                "maxDepartures", Math.max(maxDepartures, 1)
        );

        return fetchJson(properties.getEndpoints().getRealtimeDepartures(), variables)
                .map(root -> parseDepartures(root, entityNumber, stopNumber))
                .orElseGet(List::of);
    }

    // Gets scheduled departures for a specific stop and filters them to the requested travel date/time
    public List<DeLijnDepartureDto> getScheduledDepartures(String entityNumber,
                                                           String stopNumber,
                                                           LocalDate travelDate,
                                                           LocalDateTime departureTime,
                                                           int maxDepartures) {
        if (!isConfigured()
                || !StringUtils.hasText(entityNumber)
                || !StringUtils.hasText(stopNumber)
                || travelDate == null) {
            return List.of();
        }

        Map<String, Object> variables = Map.of(
                "entityNumber", entityNumber,
                "stopNumber", stopNumber,
                "date", DateTimeFormatter.ISO_LOCAL_DATE.format(travelDate),
                "maxDepartures", Math.max(maxDepartures, 1)
        );

        return fetchJson(properties.getEndpoints().getScheduledDepartures(), variables)
                .map(root -> parseDepartures(root, entityNumber, stopNumber))
                .orElseGet(List::of)
                .stream()
                // The scheduled endpoint can return cancelled or earlier departures, so the UI only receives useful options
                .filter(departure -> !departure.isCancelled())
                .filter(departure -> departure.getScheduledDepartureTime() != null)
                .filter(departure -> departure.getScheduledDepartureTime().toLocalDate().equals(travelDate))
                .filter(departure -> departureTime == null || !departure.getScheduledDepartureTime().isBefore(departureTime))
                .sorted(Comparator.comparing(DeLijnDepartureDto::getScheduledDepartureTime))
                .limit(Math.max(maxDepartures, 1))
                .toList();
    }

    // Searches stops by text and expands ambiguous terms with Antwerp/metro hints
    public List<DeLijnStopDto> searchStops(String query,
                                           Double latitude,
                                           Double longitude,
                                           int maxResults) {
        if (!isConfigured() || !StringUtils.hasText(query)) {
            return List.of();
        }

        Map<String, DeLijnStopDto> stopsByKey = new LinkedHashMap<>();
        for (String searchQuery : stopSearchQueries(query)) {
            addStops(stopsByKey, searchStopsExact(searchQuery, latitude, longitude, maxResults));
            // Stop once enough unique stop candidates have been collected
            if (stopsByKey.size() >= maxResults) {
                break;
            }
        }

        return stopsByKey.values().stream()
                .sorted(Comparator.comparing(
                        DeLijnStopDto::getDistanceMeters,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(Math.max(maxResults, 1))
                .toList();
    }

    // Runs one exact De Lijn stop search query, optionally biased by a nearby coordinate
    private List<DeLijnStopDto> searchStopsExact(String query,
                                                 Double latitude,
                                                 Double longitude,
                                                 int maxResults) {
        // Antwerp is used as the fallback because the app data and KdG routes are Antwerp-centered
        String position = hasCoordinatePair(latitude, longitude)
                ? latitude + "," + longitude
                : "51.2194,4.4025";
        Map<String, Object> variables = Map.of(
                "query", query,
                "position", position,
                "maxResults", Math.max(maxResults, 1)
        );

        return fetchJson(properties.getEndpoints().getSearchStops(), variables)
                .map(this::parseStops)
                .orElseGet(List::of)
                .stream()
                .limit(Math.max(maxResults, 1))
                .toList();
    }

    // Builds a small set of stop search variants so plain user input can still find metro/tram stops
    private static List<String> stopSearchQueries(String query) {
        String normalizedQuery = query.trim();
        String lowerQuery = normalizedQuery.toLowerCase();
        List<String> queries = new ArrayList<>();

        boolean hasCityOrStopQualifier = lowerQuery.contains("antwerp")
                || lowerQuery.contains("antwerpen")
                || lowerQuery.contains("metro")
                || lowerQuery.contains("premetro")
                || lowerQuery.contains("tram")
                || lowerQuery.contains("station")
                || lowerQuery.contains("perron");

        if (!hasCityOrStopQualifier) {
            addSearchQuery(queries, "Antwerpen " + normalizedQuery + " Metro");
            addSearchQuery(queries, "Antwerpen " + normalizedQuery);
            addSearchQuery(queries, normalizedQuery + " Metro");
        }

        addSearchQuery(queries, normalizedQuery);
        return queries;
    }

    // Adds a search query only once and skips blank variants
    private static void addSearchQuery(List<String> queries, String query) {
        if (StringUtils.hasText(query) && !queries.contains(query)) {
            queries.add(query);
        }
    }

    // Returns diagnostic information for the real-time departures endpoint
    public DeLijnDebugDto debugRealtimeDepartures(String entityNumber, String stopNumber, int maxDepartures) {
        return debugPath(
                properties.getEndpoints().getRealtimeDepartures(),
                Map.of(
                        "entityNumber", entityNumber,
                        "stopNumber", stopNumber,
                        "maxDepartures", Math.max(maxDepartures, 1)
                ),
                body -> parseRawDepartureCount(body, entityNumber, stopNumber),
                false
        );
    }

    // Gets route options without optional labels
    public List<DeLijnRouteOptionDto> getRouteOptions(Double originLatitude,
                                                      Double originLongitude,
                                                      Double destinationLatitude,
                                                      Double destinationLongitude,
                                                      LocalDateTime departureTime,
                                                      LocalDateTime arriveBefore) {
        return getRouteOptions(
                originLatitude,
                originLongitude,
                null,
                destinationLatitude,
                destinationLongitude,
                null,
                departureTime,
                arriveBefore
        );
    }

    // Gets route options either from a route planner endpoint or from nearby stop departure fallbacks
    public List<DeLijnRouteOptionDto> getRouteOptions(Double originLatitude,
                                                      Double originLongitude,
                                                      String originLabel,
                                                      Double destinationLatitude,
                                                      Double destinationLongitude,
                                                      String destinationLabel,
                                                      LocalDateTime departureTime,
                                                      LocalDateTime arriveBefore) {
        if (!isConfigured()
                || originLatitude == null
                || originLongitude == null
                || destinationLatitude == null
                || destinationLongitude == null) {
            return List.of();
        }

        if (StringUtils.hasText(properties.getEndpoints().getRouteOptions())) {
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("originLatitude", originLatitude);
            variables.put("originLongitude", originLongitude);
            variables.put("destinationLatitude", destinationLatitude);
            variables.put("destinationLongitude", destinationLongitude);
            variables.put("departureTime", departureTime);
            variables.put("arriveBefore", arriveBefore);

            return fetchJson(properties.getEndpoints().getRouteOptions(), variables)
                    .map(root -> parseRouteOptions(root, arriveBefore))
                    .orElseGet(List::of)
                    .stream()
                    .filter(routeOption -> departsAtOrAfter(routeOption.getDepartureTime(), departureTime))
                    .sorted(Comparator.comparing(
                            DeLijnRouteOptionDto::getDepartureTime,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .toList();
        }

        // If no full route planner endpoint is available, approximate options using nearby stops and departures
        return buildBasicRouteOptionsFromStops(
                originLatitude,
                originLongitude,
                originLabel,
                destinationLatitude,
                destinationLongitude,
                destinationLabel,
                departureTime,
                arriveBefore
        );
    }

    // Fetches details for one De Lijn stop
    public Optional<DeLijnStopDto> getStopDetails(String entityNumber, String stopNumber) {
        if (!isConfigured() || !StringUtils.hasText(entityNumber) || !StringUtils.hasText(stopNumber)) {
            return Optional.empty();
        }

        Map<String, Object> variables = Map.of(
                "entityNumber", entityNumber,
                "stopNumber", stopNumber
        );

        return fetchJson(properties.getEndpoints().getStopDetails(), variables)
                .map(this::parseStop)
                .filter(Objects::nonNull);
    }

    // Returns diagnostic information for one stop details request.
    public DeLijnDebugDto debugStopDetails(String entityNumber, String stopNumber) {
        return debugPath(
                properties.getEndpoints().getStopDetails(),
                Map.of(
                        "entityNumber", entityNumber,
                        "stopNumber", stopNumber
                ),
                this::parseRawStopCount,
                true
        );
    }

    // Probes known De Lijn core endpoint variants to help discover which path works for an API subscription.
    public List<DeLijnDebugDto> probeCoreEndpoints(String entityNumber, String stopNumber, int maxDepartures) {
        int safeMaxDepartures = Math.max(maxDepartures, 1);

        List<CoreEndpointProbe> probes = List.of(
                new CoreEndpointProbe(
                        "/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}/real-time?maxAantalDoorkomsten={maxDepartures}",
                        false
                ),
                new CoreEndpointProbe(
                        "/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}",
                        true
                ),
                new CoreEndpointProbe(
                        "/DLKernOpenData/v1/beta/haltes/{entityNumber}/{stopNumber}/real-time?maxAantalDoorkomsten={maxDepartures}",
                        false
                ),
                new CoreEndpointProbe(
                        "/DLKernOpenData/v1/beta/haltes/{entityNumber}/{stopNumber}",
                        true
                ),
                new CoreEndpointProbe(
                        "/DLKernOpenData/v1/haltes/{entityNumber}/{stopNumber}/real-time?maxAantalDoorkomsten={maxDepartures}",
                        false
                ),
                new CoreEndpointProbe(
                        "/DLKernOpenData/v1/haltes/{entityNumber}/{stopNumber}",
                        true
                )
        );

        Map<String, Object> variables = Map.of(
                "entityNumber", entityNumber,
                "stopNumber", stopNumber,
                "maxDepartures", safeMaxDepartures
        );

        return probes.stream()
                .map(probe -> debugPath(
                        probe.path(),
                        variables,
                        probe.stopDetails()
                                ? this::parseRawStopCount
                                : body -> parseRawDepartureCount(body, entityNumber, stopNumber),
                        probe.stopDetails()
                ))
                .toList();
    }

    // Fetches JSON from De Lijn and normalizes failures to Optional.empty for graceful UI fallback
    private Optional<JsonNode> fetchJson(String path, Map<String, Object> uriVariables) {
        if (!StringUtils.hasText(path)) {
            return Optional.empty();
        }

        try {
            String body = restClient.get()
                    .uri(path, uriVariables)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(body)) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readTree(body));
        } catch (RestClientResponseException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "De Lijn API returned HTTP " + exception.getStatusCode().value(),
                    exception
            );
            return Optional.empty();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "De Lijn API unavailable", exception);
            return Optional.empty();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "De Lijn API returned invalid JSON", exception);
            return Optional.empty();
        }
    }

    // Parses stop lists from the known De Lijn response wrappers or from a single stop object.
    private List<DeLijnStopDto> parseStops(JsonNode root) {
        JsonNode stopsNode = firstArray(root, "haltes", "stops", "results", "locaties", "items")
                .orElse(root.isArray() ? root : null);

        if (stopsNode == null || !stopsNode.isArray()) {
            DeLijnStopDto stop = parseStop(root);
            return stop == null ? List.of() : List.of(stop);
        }

        List<DeLijnStopDto> stops = new ArrayList<>();
        for (JsonNode stopNode : stopsNode) {
            DeLijnStopDto stop = parseStop(stopNode);
            if (stop != null) {
                stops.add(stop);
            }
        }

        return stops;
    }

    // Converts one De Lijn stop-like JSON object into the app stop DTO.
    private DeLijnStopDto parseStop(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode actualNode = firstObject(node, "halte", "stop", "location").orElse(node);
        Double latitude = firstDouble(
                actualNode,
                "geoCoordinaat.latitude",
                "geoCoordinaat.lat",
                "coordinate.latitude",
                "coordinaat.latitude",
                "latitude",
                "lat"
        );
        Double longitude = firstDouble(
                actualNode,
                "geoCoordinaat.longitude",
                "geoCoordinaat.lon",
                "coordinate.longitude",
                "coordinaat.longitude",
                "longitude",
                "lon"
        );

        String stopNumber = firstText(actualNode, "haltenummer", "halteNummer", "stopNumber", "nummer", "id");
        String entityNumber = firstText(actualNode, "entiteitnummer", "entiteitNummer", "entityNumber", "entiteit");

        if (!StringUtils.hasText(stopNumber) || !StringUtils.hasText(entityNumber)) {
            // Some APIs expose the entity/stop numbers only inside a composite stop key or link URL
            String stopKey = firstText(actualNode, "halteSleutel", "halte sleutel", "stopKey");
            StopKeyParts stopKeyParts = parseStopKey(valueOrFallback(stopKey, firstStopLink(actualNode)));
            stopNumber = valueOrFallback(stopNumber, stopKeyParts.stopNumber());
            entityNumber = valueOrFallback(entityNumber, stopKeyParts.entityNumber());
        }

        if (!StringUtils.hasText(stopNumber) && !StringUtils.hasText(entityNumber)) {
            return null;
        }

        return new DeLijnStopDto(
                entityNumber,
                stopNumber,
                firstText(actualNode, "omschrijving", "name", "naam", "description"),
                firstText(actualNode, "omschrijvingGemeente", "gemeente", "municipality"),
                latitude,
                longitude,
                firstInteger(actualNode, "afstand", "distance", "distanceMeters")
        );
    }

    // Splits De Lijn composite stop keys into entity and stop number parts
    private static StopKeyParts parseStopKey(String stopKey) {
        if (!StringUtils.hasText(stopKey)) {
            return new StopKeyParts(null, null);
        }

        String normalizedStopKey = stopKey;
        int stopPathIndex = normalizedStopKey.indexOf("/haltes/");
        if (stopPathIndex >= 0) {
            normalizedStopKey = normalizedStopKey.substring(stopPathIndex + "/haltes/".length());
        }

        String[] parts = normalizedStopKey.split("[_/:-]");
        if (parts.length < 2) {
            return new StopKeyParts(null, stopKey);
        }

        return new StopKeyParts(parts[0], parts[1]);
    }

    // Finds a stop URL that can be used as a fallback source for entity and stop numbers
    private static String firstStopLink(JsonNode actualNode) {
        JsonNode linksNode = firstArray(actualNode, "links").orElse(null);
        if (linksNode == null || !linksNode.isArray()) {
            return null;
        }

        for (JsonNode linkNode : linksNode) {
            String url = firstText(linkNode, "url", "href");
            if (StringUtils.hasText(url) && url.contains("/haltes/")) {
                return url;
            }
        }

        return null;
    }

    // Parses departure lists from both direct and stop-grouped De Lijn response shapes
    private List<DeLijnDepartureDto> parseDepartures(JsonNode root,
                                                     String fallbackEntityNumber,
                                                     String fallbackStopNumber) {
        JsonNode directDepartures = firstArray(root, "doorkomsten", "departures", "items")
                .orElse(root.isArray() ? root : null);

        if (directDepartures != null && directDepartures.isArray()) {
            List<DeLijnDepartureDto> departures = new ArrayList<>();
            for (JsonNode departureNode : directDepartures) {
                departures.add(parseDeparture(
                        departureNode,
                        fallbackEntityNumber,
                        fallbackStopNumber
                ));
            }
            return departures;
        }

        JsonNode stopDepartures = firstArray(root, "halteDoorkomsten", "stopDepartures", "items")
                .orElse(root.isArray() ? root : null);

        if (stopDepartures == null || !stopDepartures.isArray()) {
            return List.of();
        }

        List<DeLijnDepartureDto> departures = new ArrayList<>();
        for (JsonNode stopDeparture : stopDepartures) {
            String entityNumber = firstText(stopDeparture, "entiteitnummer", "entiteitNummer", "entityNumber");
            String stopNumber = firstText(stopDeparture, "haltenummer", "halteNummer", "stopNumber");
            JsonNode departuresNode = firstArray(stopDeparture, "doorkomsten", "departures").orElse(null);

            if (departuresNode == null || !departuresNode.isArray()) {
                continue;
            }

            for (JsonNode departureNode : departuresNode) {
                departures.add(parseDeparture(
                        departureNode,
                        valueOrFallback(entityNumber, fallbackEntityNumber),
                        valueOrFallback(stopNumber, fallbackStopNumber)
                ));
            }
        }

        return departures;
    }

    // Converts one departure JSON object into the app departure DTO
    private DeLijnDepartureDto parseDeparture(JsonNode departureNode,
                                              String fallbackEntityNumber,
                                              String fallbackStopNumber) {
        String lineNumber = firstText(departureNode, "lijnnummer", "lijnNummer", "lineNumber");

        return new DeLijnDepartureDto(
                valueOrFallback(firstText(departureNode, "entiteitnummer", "entiteitNummer", "entityNumber"), fallbackEntityNumber),
                valueOrFallback(firstText(departureNode, "haltenummer", "halteNummer", "stopNumber"), fallbackStopNumber),
                lineNumber,
                transportTypeFromNode(departureNode, lineNumber),
                firstText(departureNode, "bestemming", "destination", "richting"),
                firstDateTime(departureNode, "dienstregelingTijdstip", "scheduledDepartureTime", "scheduledTime"),
                firstDateTime(departureNode, "real-timeTijdstip", "realTimeTijdstip", "realtimeDepartureTime"),
                firstBoolean(departureNode, "afgeschaft", "cancelled", "isCancelled")
        );
    }

    // Parses route planner results when a full De Lijn route-options endpoint is available.
    private List<DeLijnRouteOptionDto> parseRouteOptions(JsonNode root, LocalDateTime arriveBefore) {
        JsonNode routesNode = firstArray(root, "reiswegen", "routes", "routeOptions", "itineraries", "items")
                .orElse(root.isArray() ? root : null);

        if (routesNode == null || !routesNode.isArray()) {
            return List.of();
        }

        List<DeLijnRouteOptionDto> routeOptions = new ArrayList<>();
        for (JsonNode routeNode : routesNode) {
            LocalDateTime departureTime = firstDateTime(routeNode, "vertrekTijd", "departureTime");
            LocalDateTime arrivalTime = firstDateTime(routeNode, "aankomstTijd", "arrivalTime");

            routeOptions.add(new DeLijnRouteOptionDto(
                    parseStop(firstObject(routeNode, "originStop", "vertrekHalte").orElse(null)),
                    parseStop(firstObject(routeNode, "destinationStop", "aankomstHalte").orElse(null)),
                    departureTime,
                    arrivalTime,
                    parseLineNumbers(routeNode),
                    transportTypeFromNode(routeNode, firstText(routeNode, "lijnnummer", "lineNumber")),
                    firstText(routeNode, "bestemming", "destination", "richting", "lineDestination"),
                    null,
                    firstBoolean(routeNode, "realtime", "realTime"),
                    arrivesBefore(arrivalTime, arriveBefore)
            ));
        }

        return routeOptions;
    }

    // Builds approximate route suggestions by pairing nearby stops with scheduled or real-time departures
    private List<DeLijnRouteOptionDto> buildBasicRouteOptionsFromStops(Double originLatitude,
                                                                        Double originLongitude,
                                                                        String originLabel,
                                                                        Double destinationLatitude,
                                                                        Double destinationLongitude,
                                                                        String destinationLabel,
                                                                        LocalDateTime requestedDepartureTime,
                                                                        LocalDateTime arriveBefore) {
        List<DeLijnStopDto> originStops = candidateStops(originLatitude, originLongitude, originLabel);
        List<DeLijnStopDto> destinationStops = candidateStops(destinationLatitude, destinationLongitude, destinationLabel);

        if (originStops.isEmpty() || destinationStops.isEmpty()) {
            return List.of();
        }

        DeLijnStopDto destinationStop = destinationStops.getFirst();
        List<DeLijnRouteOptionDto> routeOptions = new ArrayList<>();

        LocalDate travelDate = requestedDepartureTime != null ? requestedDepartureTime.toLocalDate() : LocalDate.now();

        // Keep the first search quick by checking only the closest useful departure stops
        for (DeLijnStopDto originStop : originStops.stream().limit(MAX_DEPARTURE_STOP_CANDIDATES).toList()) {
            List<DeLijnDepartureDto> departures = getScheduledDepartures(
                    originStop.getEntityNumber(),
                    originStop.getStopNumber(),
                    travelDate,
                    requestedDepartureTime,
                    properties.getMaxDepartures()
            );

            boolean usingScheduledDepartures = !departures.isEmpty();
            if (departures.isEmpty() && travelDate.equals(LocalDate.now())) {
                // Only fall back to live departures for today; future dates should remain schedule-based.
                departures = getRealtimeDepartures(
                        originStop.getEntityNumber(),
                        originStop.getStopNumber(),
                        properties.getMaxDepartures()
                );
            }

            for (DeLijnDepartureDto departure : departures) {
                // Prefer real-time when it exists, otherwise use the scheduled departure time for ordering/display.
                LocalDateTime departureTime = departure.getRealtimeDepartureTime() != null
                        ? departure.getRealtimeDepartureTime()
                        : departure.getScheduledDepartureTime();

                routeOptions.add(new DeLijnRouteOptionDto(
                        originStop,
                        destinationStop,
                        departureTime,
                        null,
                        StringUtils.hasText(departure.getLineNumber())
                                ? List.of(departure.getLineNumber())
                                : List.of(),
                        transportTypeForDeparture(departure, originStop),
                        departure.getDestination(),
                        usingScheduledDepartures
                                ? "Scheduled departure for the TravelGroup date from a nearby De Lijn stop."
                                : "Live departure from a nearby De Lijn stop.",
                        departure.getRealtimeDepartureTime() != null,
                        arrivesBefore(null, arriveBefore)
                ));
            }
        }

        return routeOptions.stream()
                .sorted(Comparator.comparing(
                        DeLijnRouteOptionDto::getDepartureTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    // Collects likely stop candidates from both coordinate proximity and label-based search
    private List<DeLijnStopDto> candidateStops(Double latitude, Double longitude, String label) {
        Map<String, DeLijnStopDto> stopsByKey = new LinkedHashMap<>();
        addStops(stopsByKey, findNearbyStops(latitude, longitude));

        if (StringUtils.hasText(label)) {
            String normalizedLabel = label.trim();
            addStops(stopsByKey, searchStops(
                    normalizedLabel,
                    latitude,
                    longitude,
                    Math.max(properties.getMaxNearbyStops(), 25)
            ));

            int commaIndex = normalizedLabel.indexOf(',');
            if (commaIndex > 0) {
                // Also search the street/stop name before the municipality suffix
                addStops(stopsByKey, searchStops(
                        normalizedLabel.substring(0, commaIndex).trim(),
                        latitude,
                        longitude,
                        Math.max(properties.getMaxNearbyStops(), 25)
                ));
            }
        }

        return stopsByKey.values().stream()
                .sorted(Comparator.comparing(
                        DeLijnStopDto::getDistanceMeters,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(Math.max(properties.getMaxNearbyStops(), 20))
                .toList();
    }

    // Adds stops to a keyed map so repeated stop search strategies do not duplicate cards
    private static void addStops(Map<String, DeLijnStopDto> stopsByKey, List<DeLijnStopDto> stops) {
        for (DeLijnStopDto stop : stops) {
            String key = stopKey(stop);
            if (StringUtils.hasText(key)) {
                stopsByKey.putIfAbsent(key, stop);
            }
        }
    }

    // Builds a stable unique key for stops returned by different De Lijn APIs
    private static String stopKey(DeLijnStopDto stop) {
        if (stop == null) {
            return null;
        }

        if (StringUtils.hasText(stop.getEntityNumber()) && StringUtils.hasText(stop.getStopNumber())) {
            return stop.getEntityNumber() + ":" + stop.getStopNumber();
        }

        if (StringUtils.hasText(stop.getName())) {
            return stop.getName() + ":" + stop.getLatitude() + ":" + stop.getLongitude();
        }

        return null;
    }

    // Extracts line numbers from route planner payloads
    private static List<String> parseLineNumbers(JsonNode routeNode) {
        JsonNode linesNode = firstArray(routeNode, "lijnen", "lines", "lineNumbers").orElse(null);
        if (linesNode == null || !linesNode.isArray()) {
            String singleLine = firstText(routeNode, "lijnnummer", "lineNumber");
            return StringUtils.hasText(singleLine) ? List.of(singleLine) : List.of();
        }

        return StreamSupport.stream(linesNode.spliterator(), false)
                .map(line -> firstText(line, "lijnnummer", "lineNumber", "nummer", "id"))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    // Chooses the most reliable transport type for a parsed departure
    private static String transportTypeForDeparture(DeLijnDepartureDto departure, DeLijnStopDto originStop) {
        String explicitTransportType = normalizeTransportType(departure.getTransportType());
        if (StringUtils.hasText(explicitTransportType)) {
            return explicitTransportType;
        }

        return inferTransportType(departure.getLineNumber(), originStop);
    }

    // Reads transport type from a route/departure node and falls back to line-number inference
    private static String transportTypeFromNode(JsonNode node, String lineNumber) {
        String explicitTransportType = normalizeTransportType(firstText(
                node,
                "vervoerType",
                "vervoertype",
                "transportType",
                "transportMode",
                "routeType",
                "route_type",
                "modus",
                "mode",
                "productType",
                "lijnType",
                "lijnsoort",
                "vlootType",
                "vloot_type"
        ));
        if (StringUtils.hasText(explicitTransportType)) {
            return explicitTransportType;
        }

        return inferTransportType(lineNumber, null);
    }

    // Normalizes known De Lijn and GTFS-like transport type values to BUS or TRAM
    private static String normalizeTransportType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        if (normalized.equals("0")
                || normalized.contains("tram")
                || normalized.contains("streetcar")
                || normalized.contains("light_rail")
                || normalized.contains("light rail")) {
            return TRANSPORT_TRAM;
        }

        if (normalized.equals("3") || normalized.contains("bus")) {
            return TRANSPORT_BUS;
        }

        return null;
    }

    // Infers bus/tram when De Lijn omits the transport type in a departure payload
    private static String inferTransportType(String lineNumber, DeLijnStopDto originStop) {
        if (!StringUtils.hasText(lineNumber)) {
            return null;
        }

        String normalizedLineNumber = lineNumber.trim().replaceFirst("^0+(?!$)", "");
        if ("0".equals(normalizedLineNumber)) {
            return TRANSPORT_TRAM;
        }

        if (originStop == null) {
            return null;
        }

        // Line numbers overlap between cities, so tram inference needs the stop municipality/name context
        String municipality = originStop.getMunicipality() == null
                ? ""
                : originStop.getMunicipality().toLowerCase();
        String stopName = originStop.getName() == null
                ? ""
                : originStop.getName().toLowerCase();
        String context = municipality + " " + stopName;

        if ((context.contains("antwerp") || context.contains("antwerpen"))
                && ANTWERP_TRAM_LINES.contains(normalizedLineNumber)) {
            return TRANSPORT_TRAM;
        }

        if ((context.contains("gent") || context.contains("ghent"))
                && GHENT_TRAM_LINES.contains(normalizedLineNumber)) {
            return TRANSPORT_TRAM;
        }

        if (context.contains("kust") || context.contains("coast")) {
            return TRANSPORT_TRAM;
        }

        return TRANSPORT_BUS;
    }

    // Finds the first array node from a list of possible JSON paths
    private static Optional<JsonNode> firstArray(JsonNode root, String... paths) {
        return firstNode(root, paths).filter(JsonNode::isArray);
    }

    // Finds the first object node from a list of possible JSON paths
    private static Optional<JsonNode> firstObject(JsonNode root, String... paths) {
        return firstNode(root, paths).filter(JsonNode::isObject);
    }

    // Tries several JSON paths because De Lijn endpoint families use different field names
    private static Optional<JsonNode> firstNode(JsonNode root, String... paths) {
        if (root == null || root.isNull()) {
            return Optional.empty();
        }

        for (String path : paths) {
            JsonNode current = root;
            for (String part : path.split("\\.")) {
                current = current.path(part);
            }

            if (!current.isMissingNode() && !current.isNull()) {
                return Optional.of(current);
            }
        }

        return Optional.empty();
    }

    // Reads the first non-blank scalar text value from possible JSON paths.
    private static String firstText(JsonNode root, String... paths) {
        return firstNode(root, paths)
                .filter(node -> !node.isContainerNode())
                .map(JsonNode::asText)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    // Reads the first numeric value from possible JSON paths.
    private static Double firstDouble(JsonNode root, String... paths) {
        return firstNode(root, paths)
                .filter(node -> node.isNumber() || node.isTextual())
                .map(JsonNode::asDouble)
                .orElse(null);
    }

    // Reads the first integer value from possible JSON paths.
    private static Integer firstInteger(JsonNode root, String... paths) {
        return firstNode(root, paths)
                .filter(node -> node.isNumber() || node.isTextual())
                .map(JsonNode::asInt)
                .orElse(null);
    }

    // Reads the first boolean value from possible JSON paths.
    private static boolean firstBoolean(JsonNode root, String... paths) {
        return firstNode(root, paths)
                .filter(node -> node.isBoolean() || node.isTextual())
                .map(JsonNode::asBoolean)
                .orElse(false);
    }

    // Parses timestamps that may be returned either with an offset or as a local date-time.
    private static LocalDateTime firstDateTime(JsonNode root, String... paths) {
        String value = firstText(root, paths);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    // Returns the primary value when present, otherwise the fallback.
    private static String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    // Checks whether a route arrives before the requested arrival time.
    private static boolean arrivesBefore(LocalDateTime arrivalTime, LocalDateTime arriveBefore) {
        return arrivalTime != null && arriveBefore != null && !arrivalTime.isAfter(arriveBefore);
    }

    // Checks that both latitude and longitude are present
    private static boolean hasCoordinatePair(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }

    // Keeps route planner results at or after the requested departure time
    private static boolean departsAtOrAfter(LocalDateTime departureTime, LocalDateTime requestedDepartureTime) {
        return departureTime == null
                || requestedDepartureTime == null
                || !departureTime.isBefore(requestedDepartureTime);
    }

    // Parses a raw stop response body into a count for debug endpoints.
    private int parseRawStopCount(String body) {
        if (!StringUtils.hasText(body)) {
            return 0;
        }

        try {
            return parseStops(objectMapper.readTree(body)).size();
        } catch (RuntimeException exception) {
            return 0;
        } catch (Exception exception) {
            return 0;
        }
    }

    // Parses a raw departure response body into a count for debug endpoints.
    private int parseRawDepartureCount(String body, String fallbackEntityNumber, String fallbackStopNumber) {
        if (!StringUtils.hasText(body)) {
            return 0;
        }

        try {
            return parseDepartures(objectMapper.readTree(body), fallbackEntityNumber, fallbackStopNumber).size();
        } catch (RuntimeException exception) {
            return 0;
        } catch (Exception exception) {
            return 0;
        }
    }

    // Executes a De Lijn endpoint and wraps response status/body/count details for debugging.
    private DeLijnDebugDto debugPath(String path,
                                     Map<String, Object> variables,
                                     DebugCountParser countParser,
                                     boolean stopCount) {
        if (!isConfigured()) {
            return new DeLijnDebugDto(
                    false,
                    path,
                    null,
                    0,
                    0,
                    null,
                    "De Lijn API key is not configured"
            );
        }

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(path, variables)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();
            int parsedCount = countParser.count(body);
            return new DeLijnDebugDto(
                    true,
                    path,
                    response.getStatusCode().value(),
                    stopCount ? parsedCount : 0,
                    stopCount ? 0 : parsedCount,
                    preview(body),
                    null
            );
        } catch (RestClientResponseException exception) {
            return new DeLijnDebugDto(
                    true,
                    path,
                    exception.getStatusCode().value(),
                    0,
                    0,
                    preview(exception.getResponseBodyAsString()),
                    "De Lijn returned HTTP " + exception.getStatusCode().value()
            );
        } catch (RuntimeException exception) {
            return new DeLijnDebugDto(
                    true,
                    path,
                    null,
                    0,
                    0,
                    null,
                    exception.getMessage()
            );
        }
    }

    @FunctionalInterface
    private interface DebugCountParser {
        int count(String body);
    }

    private record CoreEndpointProbe(String path, boolean stopDetails) {
    }

    private record StopKeyParts(String entityNumber, String stopNumber) {
    }

    // Truncates large debug response bodies so they stay readable in API responses
    private static String preview(String body) {
        if (!StringUtils.hasText(body)) {
            return body;
        }

        return body.length() <= DEBUG_BODY_PREVIEW_LENGTH
                ? body
                : body.substring(0, DEBUG_BODY_PREVIEW_LENGTH);
    }
}