# TrackTogether Documentation

TrackTogether is a Spring Boot web application for organizing activities and shared travel groups. Members can create activities, join or request access to travel groups, chat with other members, report messages, review moderation reports, view analytics, and use De Lijn data for public transport route suggestions.

This documentation is split into focused pages so the project stays easy to understand and maintain.

## Contents

- [Project Overview](docs/project-overview.md)
- [Getting Started](docs/getting-started.md)
- [Configuration](docs/configuration.md)
- [Architecture](docs/architecture.md)
- [Domain Model](docs/domain-model.md)
- [Feature Guide](docs/feature-guide.md)
- [API Reference](docs/api-reference.md)
- [De Lijn API and Route Suggestions](docs/de-lijn-api.md)
- [Security and Roles](docs/security-and-roles.md)
- [Frontend and Templates](docs/frontend-and-templates.md)
- [Deployment](docs/deployment.md)
- [Testing](docs/testing.md)
- [Troubleshooting](docs/troubleshooting.md)

## Existing Documents

The `docs` folder also contains:

- `De Lijn Route Suggestions - Feature Documentation.pdf`: existing feature documentation for the De Lijn route suggestion work.

## Quick Commands

From the project root:

```powershell
.\gradlew bootRun
```

```powershell
.\gradlew test
```

```powershell
docker compose up --build
```
