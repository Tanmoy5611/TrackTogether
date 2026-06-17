# Deployment

## Build Artifact

The Gradle Spring Boot plugin builds an executable jar:

```powershell
.\gradlew clean bootJar
```

The Dockerfile builds that jar in a Java 21 JDK image and runs it in a Java 21 JRE image.

## Dockerfile

The Dockerfile has two stages:

1. Build stage:
   - Uses `eclipse-temurin:21-jdk`.
   - Copies the project.
   - Runs `./gradlew clean bootJar -x test`.
2. Runtime stage:
   - Uses `eclipse-temurin:21-jre`.
   - Copies the generated jar to `app.jar`.
   - Sets `SERVER_PORT=80`.
   - Exposes port `80`.
   - Starts with `java -jar app.jar`.

## Docker Compose

`docker-compose.yml` defines:

| Service | Purpose |
| --- | --- |
| `postgres` | PostgreSQL database |
| `backend-1` | First TrackTogether app instance |
| `backend-2` | Second TrackTogether app instance |

Ports:

| Service | Host | Container |
| --- | --- | --- |
| `postgres` | `127.0.0.1:5052` | `5432` |
| `backend-1` | `127.0.0.1:8080` | `80` |
| `backend-2` | `127.0.0.1:8081` | `80` |

Run:

```powershell
docker compose up --build
```

Stop:

```powershell
docker compose down
```

Stop and remove the database volume:

```powershell
docker compose down -v
```

## Required Environment Variables

At minimum, provide:

```properties
DB_USERNAME=postgres
DB_PASSWORD=postgres
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_CLIENT_ID=your-google-client-id
```

`application.properties` reads `GOOGLE_CLIENT_ID` and falls back to the checked-in development client id when it is not set.

Optional De Lijn variables:

```properties
DELIJN_API_KEY=your-delijn-key
DELIJN_API_BASE_URL=https://api.delijn.be
DELIJN_API_TIMEOUT=3s
```

## Production Profile

The default Spring profile is `development`. For production-like deployment, set:

```properties
SPRING_PROFILES_ACTIVE=production
```

Production profile behavior:

- `spring.jpa.hibernate.ddl-auto=update`
- `spring.sql.init.mode=never`
- SQL logging disabled

Without setting the production profile, Docker Compose uses the default `development` profile. That means schema creation and seed data loading are active.

## Database Persistence

Compose stores PostgreSQL data in the named volume:

```text
postgres_data
```

This preserves data across container restarts. Use `docker compose down -v` only when you intentionally want to reset the database.

## Multi-Instance Notes

Compose starts two backend containers against one database. Database locking in travel group joins is important here because multiple app instances can process requests at the same time.

The Compose file does not include a load balancer. Access instances directly through:

- `http://localhost:8080`
- `http://localhost:8081`

For a production deployment, put a reverse proxy or load balancer in front of the instances and configure forwarded headers. The application already sets:

```properties
server.forward-headers-strategy=framework
```

in development and production profile files.

## Secrets

Do not commit real secrets. Use environment variables, deployment secret stores, or an untracked `.env` file.

Secrets include:

- Database password.
- Google OAuth client secret.
- De Lijn API key.
