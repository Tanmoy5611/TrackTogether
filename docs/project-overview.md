# Project Overview

## Purpose

TrackTogether helps students and staff coordinate travel to activities. The application combines event discovery, travel group planning, chat, moderation, notifications, and CO2 analytics in one Spring Boot project.

## Main Capabilities

- Google OAuth2 login with user provisioning.
- Member profiles with preferred transport mode and default departure location.
- Activity creation and verification.
- Travel group creation, editing, joining, leaving, ownership transfer, invitations, location sharing, and activity logs.
- Optional travel group owner approval before members can join.
- Friend and travel group matching based on event, transport mode, location, time, available seats, and optional De Lijn route compatibility.
- Direct chats, travel group chats, and custom group chats.
- Message reporting and moderation workflows with assignment and status history.
- Notifications for joins, leaves, group-full events, join request decisions, and new travel group chat messages.
- Member and admin analytics for trips, transport popularity, and CO2 savings.
- De Lijn integration for nearby stops, stop search, departures, and route suggestions.
- English and Dutch localization through message bundles.

## Technology Stack

| Area | Technology |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Build | Gradle Kotlin DSL |
| Web | Spring MVC, Thymeleaf |
| Security | Spring Security, OAuth2 Client, OIDC |
| Database | Spring Data JPA, PostgreSQL |
| Tests | JUnit Platform, Spring Boot Test, H2 |
| UI assets | Bootstrap, Bootstrap Icons, Leaflet |
| Deployment | Docker, Docker Compose |

## Repository Layout

```text
.
|-- build.gradle.kts
|-- docker-compose.yml
|-- Dockerfile
|-- docs/
|-- src/
|   |-- main/
|   |   |-- java/TrackTogether/
|   |   |-- resources/
|   |-- test/
|-- gradlew
|-- gradlew.bat
```

Important application folders:

| Path | Purpose |
| --- | --- |
| `src/main/java/TrackTogether/controller` | Thymeleaf MVC page controllers |
| `src/main/java/TrackTogether/webapi` | JSON REST API controllers |
| `src/main/java/TrackTogether/service` | Business logic |
| `src/main/java/TrackTogether/repository` | Spring Data JPA repositories |
| `src/main/java/TrackTogether/domain` | JPA entities and enums |
| `src/main/java/TrackTogether/security` | OAuth2, role provisioning, session and account status handling |
| `src/main/java/TrackTogether/config` | De Lijn client and locale configuration |
| `src/main/resources/templates` | Thymeleaf pages and fragments |
| `src/main/resources/static` | CSS, JavaScript, images, favicon |
| `src/main/resources/SQL/Data.sql` | Development seed data |
| `src/test/java/TrackTogether` | Automated tests |

