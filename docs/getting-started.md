# Getting Started

## Prerequisites

- Java 21.
- A shell that can run the Gradle wrapper. On Windows, use PowerShell with `.\gradlew.bat` or `.\gradlew`.
- Docker Desktop or another Docker runtime if you want to run PostgreSQL or the full stack through Compose.
- Google OAuth2 credentials for local login.
- Optional: a De Lijn API key for public transport features.

## Clone and Open

Open the repository root:

```powershell
cd C:\Users\yoran\IdeaProjects\spring-backend
```

All commands in this documentation assume they are run from that folder.

## Environment File

The application imports an optional `.env` file from the project root:

```properties
DB_USERNAME=postgres
DB_PASSWORD=postgres
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_CLIENT_ID=your-google-client-id
DELIJN_API_KEY=your-delijn-key
```

The checked-in `application.properties` contains a fallback Google client id. Set `GOOGLE_CLIENT_ID` to use your own OAuth client without editing the property file.

## Start PostgreSQL Only

For local `bootRun`, the development profile expects PostgreSQL at `localhost:5052` by default. The Compose file can start only PostgreSQL:

```powershell
docker compose up -d postgres
```

The database name is `tracktogether`. Credentials come from `DB_USERNAME` and `DB_PASSWORD`.

## Run the App Locally

```powershell
.\gradlew bootRun
```

By default, the application uses the `development` profile and listens on port `8080`.

If port `8080` is already in use locally, pass an override:

```powershell
.\gradlew bootRun --args='--server.port=8082'
```

Then open:

```text
http://localhost:8080
```

If you used the example override:

```text
http://localhost:8082
```

## Development Database Behavior

The default profile is `development`. In that profile:

- Hibernate uses `ddl-auto=create`.
- SQL seed data is loaded from `classpath:SQL/Data.sql`.
- SQL logging is enabled.
- Existing local data can be recreated on startup.

Use the `production` profile when you do not want development seed behavior.

## Run Tests

```powershell
.\gradlew test
```

Tests use an in-memory H2 database configured in `src/test/resources/application.properties`.

## Full Docker Compose Run

To build and run PostgreSQL plus two backend containers:

```powershell
docker compose up --build
```

Compose maps the backends to:

- `http://localhost:8080`
- `http://localhost:8081`

Inside each container the Docker image sets `SERVER_PORT=80`, so the application exposes port `80`.
