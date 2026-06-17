# Configuration

## Property Files

| File | Purpose |
| --- | --- |
| `src/main/resources/application.properties` | Shared defaults, OAuth2, session, messages, De Lijn settings |
| `src/main/resources/application-development.properties` | Local PostgreSQL, schema creation, seed data, SQL logging |
| `src/main/resources/application-production.properties` | PostgreSQL with `ddl-auto=update` and no seed data |
| `src/test/resources/application.properties` | H2 test database and test OAuth values |

The application imports `optional:file:.env`, so local environment variables can be stored in a root `.env` file.

## Core Settings

| Property | Default | Notes |
| --- | --- | --- |
| `spring.application.name` | `TrackTogether` | Application name |
| `spring.profiles.default` | `development` | Active profile if none is provided |
| `server.port` | `${SERVER_PORT:8080}` | App HTTP port. The Docker image sets `SERVER_PORT=80`. |
| `spring.jpa.open-in-view` | `false` | Avoids lazy loading during view rendering |
| `spring.messages.basename` | `messages` | Uses `messages.properties` and `messages_nl.properties` |
| `tracktogether.security.inactivity-logout-timeout-millis` | `1980000` | Browser inactivity logout, 33 minutes |
| `server.servlet.session.timeout` | `34m` | Server-side session timeout |

## Database Settings

Development profile:

| Property | Default |
| --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5052/tracktogether` |
| `spring.datasource.username` | `${DB_USERNAME:postgres}` |
| `spring.datasource.password` | `${DB_PASSWORD:postgres}` |
| `spring.jpa.hibernate.ddl-auto` | `create` |
| `spring.sql.init.mode` | `always` |
| `spring.sql.init.data-locations` | `classpath:SQL/Data.sql` |

Production profile:

| Property | Default |
| --- | --- |
| `spring.jpa.hibernate.ddl-auto` | `update` |
| `spring.sql.init.mode` | `never` |
| `spring.jpa.show-sql` | `false` |

## OAuth2 Settings

The app uses Google OIDC.

| Property | Notes |
| --- | --- |
| `spring.security.oauth2.client.registration.google.client-id` | Google OAuth client id. Override with `GOOGLE_CLIENT_ID` |
| `spring.security.oauth2.client.registration.google.client-secret` | Reads from `GOOGLE_CLIENT_SECRET` |
| `spring.security.oauth2.client.registration.google.scope` | `openid,profile,email` |
| `spring.security.oauth2.client.provider.google.issuer-uri` | `https://accounts.google.com` |
| `spring.security.oauth2.client.provider.google.user-name-attribute` | `sub` |

`GoogleCloudOrganizationDomainAuthorizationRequestResolver` adds `hd=kdg.be` to the Google authorization request. This is a Google hosted-domain hint for the login screen.

## De Lijn Settings

| Property | Environment variable | Default |
| --- | --- | --- |
| `delijn.api.base-url` | `DELIJN_API_BASE_URL` | `https://api.delijn.be` |
| `delijn.api.api-key` | `DELIJN_API_KEY` | empty |
| `delijn.api.timeout` | `DELIJN_API_TIMEOUT` | `3s` |
| `delijn.api.nearby-stop-radius-meters` | `DELIJN_API_NEARBY_STOP_RADIUS_METERS` | `2500` |
| `delijn.api.max-nearby-stops` | `DELIJN_API_MAX_NEARBY_STOPS` | `20` |
| `delijn.api.max-departures` | `DELIJN_API_MAX_DEPARTURES` | `8` |
| `delijn.api.endpoints.nearby-stops` | `DELIJN_API_NEARBY_STOPS_PATH` | empty |
| `delijn.api.endpoints.realtime-departures` | `DELIJN_API_REALTIME_DEPARTURES_PATH` | De Lijn real-time stop endpoint |
| `delijn.api.endpoints.scheduled-departures` | `DELIJN_API_SCHEDULED_DEPARTURES_PATH` | De Lijn schedule endpoint |
| `delijn.api.endpoints.stop-details` | `DELIJN_API_STOP_DETAILS_PATH` | De Lijn stop details endpoint |
| `delijn.api.endpoints.search-stops` | `DELIJN_API_SEARCH_STOPS_PATH` | De Lijn stop search endpoint |
| `delijn.api.endpoints.route-options` | `DELIJN_API_ROUTE_OPTIONS_PATH` | empty |

If `DELIJN_API_KEY` is empty, De Lijn API endpoints return service-unavailable style errors or route suggestion responses marked as not configured.

## Localization

`LocaleConfig` uses a cookie named `TRACKTOGETHER_LOCALE` and defaults to English. The locale can be changed through the `lang` request parameter.

Message bundles:

- `src/main/resources/messages.properties`
- `src/main/resources/messages_nl.properties`

## System Settings Stored in the Database

`SystemSettings` is a singleton row with id `1`.

| Setting | Meaning | Default |
| --- | --- | --- |
| `travelGroupJoinApprovalEnabled` | When true, joining creates a pending request that the group owner must accept. When false, members join directly. | `false` |
