# Troubleshooting

## Port 80 Is Already In Use

The app defaults to port `80`.

Run with another port:

```powershell
.\gradlew bootRun --args='--server.port=8080'
```

Then open:

```text
http://localhost:8080
```

## Cannot Connect To PostgreSQL

The development profile expects:

```text
jdbc:postgresql://localhost:5052/tracktogether
```

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Check `.env` values:

```properties
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Database Keeps Resetting

The default profile is `development`, where:

```properties
spring.jpa.hibernate.ddl-auto=create
spring.sql.init.mode=always
```

Use the production profile to avoid development reset behavior:

```powershell
.\gradlew bootRun --args='--spring.profiles.active=production'
```

## Google Login Fails

Check:

- `GOOGLE_CLIENT_SECRET` is set.
- The Google client id is correct.
- The OAuth redirect URI matches the host and port you use locally.
- The browser is opening the same port configured in Google Cloud.

Common local redirect URI examples:

```text
http://localhost/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/google
```

## User Is Redirected To Banned Page

The member's `status` is false. A super admin can reactivate the account through user management.

When a status changes, active sessions for that user are expired.

## De Lijn Endpoints Return Service Unavailable

Set:

```properties
DELIJN_API_KEY=your-delijn-key
```

Then restart the app.

If the API key is set but no data appears, use debug endpoints:

```text
/api/delijn/nearby-stops/debug
/api/delijn/stops/{entityNumber}/{stopNumber}/debug
/api/delijn/stops/{entityNumber}/{stopNumber}/departures/debug
/api/delijn/stops/{entityNumber}/{stopNumber}/probe
```

## Route Suggestions Say Public Transport Only

Route suggestions are only supported for travel groups with:

```text
PUBLIC_TRANSPORT
```

Change the travel group's transport mode or use a public transport group.

## Route Suggestions Say Coordinates Are Missing

The group needs valid origin and destination coordinates.

Origin usually comes from:

- `departureLatitude`
- `departureLongitude`

Destination usually comes from:

- `arrivalLatitude`
- `arrivalLongitude`
- or the linked activity's latitude/longitude

## Cannot Join A Travel Group

Possible reasons:

- You are already a member.
- You are the owner.
- The group is full.
- Join approval is enabled and you already have a pending request.
- The linked activity is not visible to you.

## Owner Cannot Leave A Travel Group

Owners must transfer ownership before leaving.

If the owner is the only member, they can delete the group instead.

## Cannot Delete A Travel Group

Only the owner can delete a travel group, and only when they are the only member.

## Cannot Manage A Custom Group Chat

Only custom group owners can:

- Rename the group.
- Add members.
- Remove members.
- Change member roles.

The app prevents removing or demoting the final owner.

## Moderator Cannot Update A Report

If a report is already assigned to another moderator, the current moderator cannot update its status.

An admin can assign the report to a different moderator.

## Activity Not Visible

Only approved activities are visible to everyone.

Pending or disapproved activities are visible only to their creator.

Moderators can approve or disapprove activities from the admin activity management flow.

