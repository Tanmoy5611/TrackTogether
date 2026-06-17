# TrackTogether Documentation

TrackTogether is a Spring Boot web application for organizing activities and shared travel groups. Members can create activities, join or request access to travel groups, chat with other members, report messages, review moderation reports, view analytics, and use De Lijn data for public transport route suggestions.

This documentation is split into focused pages so the project stays easy to understand and maintain.

## Contents

- [Project Overview](project-overview.md)
- [Getting Started](getting-started.md)
- [Configuration](configuration.md)
- [Architecture](architecture.md)
- [Domain Model](domain-model.md)
- [Feature Guide](feature-guide.md)
- [API Reference](api-reference.md)
- [De Lijn API and Route Suggestions](de-lijn-api.md)
- [Security and Roles](security-and-roles.md)
- [Frontend and Templates](frontend-and-templates.md)
- [Deployment](deployment.md)
- [Testing](testing.md)
- [Troubleshooting](troubleshooting.md)

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
