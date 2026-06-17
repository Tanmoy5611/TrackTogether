# Testing

## Test Command

Run all tests:

```powershell
.\gradlew test
```

The Gradle build uses JUnit Platform.

## Test Configuration

Test properties live in:

```text
src/test/resources/application.properties
```

Important test settings:

| Setting | Value |
| --- | --- |
| `spring.profiles.active` | `test` |
| `server.port` | `0` |
| Database | H2 in-memory |
| H2 mode | PostgreSQL compatibility |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` |
| SQL init | disabled |
| OAuth client id/secret | test values |
| De Lijn API key | empty |

## Existing Test Areas

| Test file | Area |
| --- | --- |
| `TrackTogetherApplicationTests` | Application context |
| `TravelGroupApiControllerTest` | Travel group REST API |
| `TravelGroupServiceTest` | Travel group workflows |
| `TravelGroupRouteSuggestionServiceTest` | De Lijn route suggestion wrapper |
| `MemberServiceTest` | Member preferences and lookup |
| `GoogleCalendarLinkServiceTest` | Calendar URL generation |
| `FriendMatchingServiceTest` | Travel group matching |
| `AccountStatusAccessFilterTest` | Disabled account filter behavior |
| `ActivityServiceTest` | Activity visibility and persistence |
| `ActivityPolicyServiceTest` | Activity policy rules |
| `AnalyticsServiceTest` | Analytics and CO2 calculations |

## What To Test When Changing Features

### Travel Groups

Add or update tests when changing:

- Create/edit validation.
- Join flow.
- Join approval behavior.
- Capacity checks.
- Owner-only actions.
- Ownership transfer.
- Delete and leave cleanup.
- Activity visibility filtering.
- Notifications triggered by travel group events.

### Chat

Add tests when changing:

- Direct conversation creation.
- Travel group conversation access.
- Custom group creation.
- Owner/member role behavior.
- Last owner protection.
- Message sending.

### Moderation

Add tests when changing:

- Report creation validation.
- Duplicate report prevention.
- Moderator claim flow.
- Status transitions.
- Report history.
- Chat context window.

### Analytics

Add tests when changing:

- CO2 factors.
- Default distance behavior.
- Carpool calculations.
- Time period grouping.
- Transport mode display.

### De Lijn

Add tests when changing:

- Missing API key behavior.
- Endpoint path configuration.
- Parsing response shapes.
- Fallback route suggestion behavior.
- Max result limits and coordinate validation.

## Test Database Notes

The H2 URL is:

```properties
jdbc:h2:mem:tracktogether-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
```

This allows tests to mimic PostgreSQL behavior closely enough for repository and service tests, but it is not a perfect PostgreSQL substitute. Test locking or database-specific SQL carefully.

## Build Checks

For documentation-only changes, tests are usually optional. For Java changes, run:

```powershell
.\gradlew test
```

For Docker-related changes, run:

```powershell
docker compose up --build
```

and verify both backend ports.

