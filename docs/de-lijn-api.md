# De Lijn API and Route Suggestions

This page is the Markdown companion to `De Lijn Route Suggestions - Feature Documentation.pdf`. It documents the current De Lijn integration as implemented in the project.

## Overview

The De Lijn feature gives public transport travel groups bus and tram departure suggestions. It combines:

- The travel group's departure point.
- The linked activity destination.
- Optional user-selected origin, destination, and departure time.
- De Lijn stop search, stop details, scheduled departures, live departures, or a configured route planner endpoint.

The main browser page is:

```text
/travelgroups/{groupId}/route-suggestions
```

The main JSON endpoint is:

```text
GET /api/travelgroups/{groupId}/route-suggestions
```

Route suggestions are supported only for travel groups with:

```text
PUBLIC_TRANSPORT
```

## Files Involved

### Backend

| File | Responsibility |
| --- | --- |
| `TravelGroupController.java` | Renders the route suggestion page and exposes shared location form routes |
| `TravelGroupApiController.java` | Exposes the JSON route suggestion endpoint |
| `DeLijnApiController.java` | Exposes De Lijn helper, debug, stop, and departure endpoints |
| `TravelGroupRouteSuggestionService.java` | Validates and orchestrates route suggestion responses |
| `DeLijnService.java` | Calls De Lijn APIs and parses stop, departure, and route payloads |
| `FriendMatchingService.java` | Uses De Lijn route data as an extra public transport matching signal |
| `DeLijnConfig.java` | Builds the configured `RestClient` |
| `DeLijnProperties.java` | Maps `delijn.api.*` configuration |

### Frontend

| File | Responsibility |
| --- | --- |
| `travelgroup-route-suggestions.html` | Route suggestion page |
| `travelgroup-route-suggestions.js` | Route form, loading state, stop autocomplete, route cards, Leaflet map |
| `travelgroup-route-footer.js` | Route page footer behavior |
| `travelgroup-map.js` | Google and Apple map links for group/activity locations |
| `travelgroup-location-share.js` | Shared member location behavior |
| `travelgroup-detail.html` | Travel group detail and route link entry point |

## Main User Flow

1. A user opens a travel group detail page.
2. If the group uses `PUBLIC_TRANSPORT`, the route suggestions action is available.
3. The user opens `/travelgroups/{groupId}/route-suggestions`.
4. `TravelGroupController` loads the group and renders the page.
5. Thymeleaf writes default route values into `data-*` attributes.
6. `travelgroup-route-suggestions.js` reads those defaults.
7. The script calls `/api/travelgroups/{groupId}/route-suggestions`.
8. `TravelGroupApiController` delegates to `TravelGroupRouteSuggestionService`.
9. The service validates the group, transport mode, coordinates, result count, and De Lijn configuration.
10. `DeLijnService` fetches route options or builds fallback options from stops and departures.
11. The route suggestion service balances bus and tram options when possible.
12. The API returns `TravelGroupRouteSuggestionsDto`.
13. The frontend renders status text, route cards, and the Leaflet map.

## Main Route Suggestion Endpoint

```http
GET /api/travelgroups/{groupId}/route-suggestions
```

Query parameters:

| Parameter | Default | Notes |
| --- | --- | --- |
| `maxResults` | `4` | Must be between 1 and 25. The route page normally sends 8. |
| `originLatitude` | Group departure latitude | Optional route origin override |
| `originLongitude` | Group departure longitude | Optional route origin override |
| `originLabel` | Group departure location | Optional route origin label |
| `destinationLatitude` | Activity or group arrival latitude | Optional destination override |
| `destinationLongitude` | Activity or group arrival longitude | Optional destination override |
| `destinationLabel` | Activity location | Optional destination label |
| `departureTime` | Group departure time | Optional ISO date-time override |

Response type:

```text
TravelGroupRouteSuggestionsDto
```

Important response fields:

| Field | Meaning |
| --- | --- |
| `supported` | Whether route suggestions apply to this group |
| `configured` | Whether De Lijn API access is configured |
| `message` | User-facing status message |
| `originLabel` | Origin label used for the query |
| `originLatitude` | Origin latitude used for the query |
| `originLongitude` | Origin longitude used for the query |
| `destinationLabel` | Destination label used for the query |
| `destinationLatitude` | Destination latitude used for the query |
| `destinationLongitude` | Destination longitude used for the query |
| `transitCoverage` | Text describing De Lijn bus/tram coverage |
| `options` | List of `DeLijnRouteOptionDto` values |

## Configuration

Configuration class:

```text
TrackTogether.config.DeLijnProperties
```

Prefix:

```text
delijn.api
```

Main properties:

| Property | Environment variable | Default |
| --- | --- | --- |
| `delijn.api.base-url` | `DELIJN_API_BASE_URL` | `https://api.delijn.be` |
| `delijn.api.api-key` | `DELIJN_API_KEY` | empty |
| `delijn.api.timeout` | `DELIJN_API_TIMEOUT` | `3s` |
| `delijn.api.nearby-stop-radius-meters` | `DELIJN_API_NEARBY_STOP_RADIUS_METERS` | `2500` |
| `delijn.api.max-nearby-stops` | `DELIJN_API_MAX_NEARBY_STOPS` | `20` |
| `delijn.api.max-departures` | `DELIJN_API_MAX_DEPARTURES` | `8` |
| `delijn.api.endpoints.nearby-stops` | `DELIJN_API_NEARBY_STOPS_PATH` | empty |
| `delijn.api.endpoints.realtime-departures` | `DELIJN_API_REALTIME_DEPARTURES_PATH` | `/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}/real-time?maxAantalDoorkomsten={maxDepartures}` |
| `delijn.api.endpoints.scheduled-departures` | `DELIJN_API_SCHEDULED_DEPARTURES_PATH` | `/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}/dienstregelingen?datum={date}` |
| `delijn.api.endpoints.stop-details` | `DELIJN_API_STOP_DETAILS_PATH` | `/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}` |
| `delijn.api.endpoints.search-stops` | `DELIJN_API_SEARCH_STOPS_PATH` | `/DLZoekOpenData/v1/zoek/haltes/{query}?huidigePositie={position}&maxAantalHits={maxResults}` |
| `delijn.api.endpoints.route-options` | `DELIJN_API_ROUTE_OPTIONS_PATH` | empty |

The application sends the subscription key as:

```text
Ocp-Apim-Subscription-Key
```

## Route Option Strategy

The main backend method is:

```text
DeLijnService.getRouteOptions(...)
```

### Path 1: Configured Route Planner Endpoint

If `delijn.api.endpoints.route-options` is set, the service calls that route planner endpoint directly.

It then:

- Parses route options.
- Filters out options before the selected departure time.
- Sorts by departure time.
- Returns `DeLijnRouteOptionDto` values.

### Path 2: Fallback From Stops And Departures

By default, `route-options` is empty, so the fallback path is normally active.

The fallback:

1. Finds candidate stops near the origin coordinates.
2. Searches stops using the origin label.
3. Finds candidate stops near the destination coordinates.
4. Searches stops using the destination label.
5. Picks the first destination-side stop candidate.
6. For future dates, fetches scheduled departures.
7. For today's date, falls back to live departures if scheduled departures are empty.
8. Builds route cards from the closest useful departure stops.
9. Sorts results by departure time.

Known implication: this fallback does not guarantee a full A-to-B journey plan. It gives useful nearby De Lijn departure options.

## De Lijn Helper Endpoints

Base path:

```text
/api/delijn
```

| Endpoint | Purpose |
| --- | --- |
| `GET /api/delijn/nearby-stops` | Find nearby stops when a nearby-stops endpoint is configured |
| `GET /api/delijn/nearby-stops/debug` | Debug the configured nearby-stops path and payload |
| `GET /api/delijn/stop-suggestions` | Stop autocomplete for route text inputs |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}/scheduled-departures` | Scheduled departures for a stop/date |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}` | Stop details |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}/debug` | Stop details diagnostics |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}/probe` | Probe possible De Lijn endpoint variants |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}/departures` | Real-time departures |
| `GET /api/delijn/stops/{entityNumber}/{stopNumber}/departures/debug` | Real-time departure diagnostics |

### Nearby Stops

```http
GET /api/delijn/nearby-stops?lat=51.2172&lng=4.4211&radiusMeters=1000&maxResults=5
```

Validation:

- `lat`: -90 to 90.
- `lng`: -180 to 180.
- `radiusMeters`: 1 to 5000.
- `maxResults`: 1 to 25.

### Stop Suggestions

```http
GET /api/delijn/stop-suggestions?q=Groenplaats&lat=51.2194&lng=4.4025&maxResults=15
```

Notes:

- Query shorter than 2 characters returns an empty list.
- Coordinates are optional.
- If one coordinate is supplied, both must be valid.
- Search is biased by the supplied coordinate when available.
- Without coordinates, Antwerp is used as the fallback search position because the demo data is Antwerp-centered.

### Scheduled Departures

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/scheduled-departures?date=2026-07-09&departureTime=2026-07-09T13:10:00&maxResults=8
```

The service filters scheduled departures to:

- The requested travel date.
- Departures at or after the optional `departureTime`.
- Non-cancelled departures.

### Real-Time Departures

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/departures?maxResults=4
```

This returns live departures for the stop.

### Probe Endpoint

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/probe?maxResults=4
```

Use this when a subscription or endpoint path is uncertain. It tests known De Lijn core endpoint variants and returns debug DTOs with status, counts, preview text, and errors.

## Frontend Behavior

Main script:

```text
src/main/resources/static/js/travelgroup-route-suggestions.js
```

On page load it:

- Reads route defaults from `data-*` attributes.
- Initializes origin, destination, and departure fields.
- Calls the backend route suggestion endpoint.
- Shows skeleton loading cards while waiting.
- Lazily initializes a Leaflet map.
- Draws origin and destination markers.
- Draws a straight route line between the two points.
- Renders a route summary and departure cards.

User actions:

- Submit the form to search with new origin, destination, or departure time.
- Swap origin and destination.
- Reset to group defaults.
- Use the current time.
- Select a route card to focus it on the map.
- Open a route in Google Maps transit mode.

Stop input behavior:

- Calls `/api/delijn/stop-suggestions`.
- Stores returned stop candidates.
- Resolves typed values to De Lijn stop coordinates where possible.
- Falls back to OpenStreetMap Nominatim geocoding when stop search cannot resolve typed text.

## Map Links And Shared Locations

The broader route feature also supports map links and member pickup sharing.

Shared location routes:

| Endpoint | Purpose |
| --- | --- |
| `POST /travelgroups/{groupId}/location` | Save a manual shared location |
| `POST /travelgroups/{groupId}/location/live` | AJAX endpoint for browser location updates |
| `POST /travelgroups/{groupId}/location/clear` | Clear the current member's shared location |

Shared locations are stored on the member's `TravelGroupMember` row through a `Location` entity.

## Public Transport Friend Matching

`FriendMatchingService` uses De Lijn data as an extra layer on top of normal matching.

Normal matching still considers:

- Same activity.
- Transport mode.
- Compatible transport modes.
- Nearby departure location.
- Similar departure time.
- Available seats.
- Exclusion of full, joined, pending, owned, invisible, or past groups.

De Lijn matching runs when:

- The candidate group uses `PUBLIC_TRANSPORT`.
- The current member's preference or explicit filter is compatible with public transport.
- `DeLijnService` is configured with an API key.
- Member, group, and destination coordinates are available.
- The per-request De Lijn comparison limit has not been reached.

Public transport score signals:

| Signal | Score |
| --- | --- |
| Same De Lijn origin stop | 30 |
| Shared De Lijn line | 25 |
| Route arrives before the event | 15 |
| Similar route departure time | 15 |

## Manual Testing

Recommended flow:

1. Configure `DELIJN_API_KEY`.
2. Start the app.
3. Open a `PUBLIC_TRANSPORT` travel group.
4. Open the De Lijn route suggestion page.
5. Confirm skeleton cards appear while data loads.
6. Confirm scheduled or live departure cards appear when data exists.
7. Try reset, swap, use-now, stop suggestions, and Google Maps transit links.
8. Open a non-public-transport group and confirm the unsupported message appears.
9. Clear `DELIJN_API_KEY`, restart, and confirm the not-configured message appears.

Seed data in `Data.sql` includes Antwerp-centered members, activities, public transport groups, full groups, and past groups that are useful for demos.

## Troubleshooting

### API key missing

Set:

```properties
DELIJN_API_KEY=your-key
```

Then restart the app.

### Route suggestions show "public transport only"

The travel group must use:

```text
PUBLIC_TRANSPORT
```

### Route suggestions show missing coordinates

Ensure the group has:

- `departureLatitude`
- `departureLongitude`

and the destination has either:

- `arrivalLatitude` and `arrivalLongitude`
- or the linked activity has latitude and longitude.

### No departures for future dates

Future dates depend on scheduled departures. Real-time fallback only applies when the selected date is today.

### Nearby stops endpoint does not work

`delijn.api.endpoints.nearby-stops` is empty by default. Configure `DELIJN_API_NEARBY_STOPS_PATH` if using that endpoint directly.

### Endpoint path uncertainty

Use:

```text
/api/delijn/stops/{entityNumber}/{stopNumber}/probe
```

The response shows status codes, parsed stop/departure counts, body previews, and errors.

## Known Limitations

| Limitation | Reason |
| --- | --- |
| No guaranteed full A-to-B route planning by default | The default fallback uses nearby stops and departure data, not a full journey planner |
| Map route geometry is approximate | Leaflet draws a straight line between selected route points |
| Destination stop is approximate | The fallback picks the first viable destination-side stop |
| Future dates depend on scheduled departures | Real-time data only makes sense for today |
| Typed location fallback uses Nominatim | It is used when De Lijn stop search cannot resolve the text |
| Live location is page-scoped | Browser live tracking stops when the detail page closes |

