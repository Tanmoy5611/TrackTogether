# API Reference

All API routes require an authenticated session unless explicitly public in Spring Security. Browser-based calls should include the normal Spring Security CSRF token for unsafe methods when CSRF protection applies.

Error responses from `ApiExceptionHandler` generally use this shape:

```json
{
  "status": 400,
  "message": "Validation error",
  "path": "/api/example",
  "timestamp": "2026-06-02T12:00:00"
}
```

## Common Enum Values

`TransportMode`:

```text
CAR
CARPOOL
BIKE
WALK
PUBLIC_TRANSPORT
```

`ActivityVerificationStatus`:

```text
PENDING
APPROVED
DISAPPROVED
```

`ReportStatus`:

```text
OPEN
REVIEWED
RESOLVED
REJECTED
```

## Travel Group API

Base path: `/api/travelgroups`

### Get Suggested Travel Groups

```http
GET /api/travelgroups/suggestions
```

Optional query parameters:

| Name | Type | Notes |
| --- | --- | --- |
| `activityId` | UUID | Restrict suggestions to one activity |
| `transportMode` | `TransportMode` | Restrict suggestions to one transport mode |

Returns a list of `TravelFriendSuggestionDto`.

### Create Travel Group

```http
POST /api/travelgroups
```

Request body:

```json
{
  "activityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1",
  "maxMembers": 4,
  "location": "Antwerp Central Station",
  "departureLocation": "Antwerp Central Station",
  "departureLatitude": 51.2172,
  "departureLongitude": 4.4211,
  "departureTime": "2026-07-09T13:10:00",
  "estimatedArrivalTime": "2026-07-09T13:45:00",
  "transportMode": "PUBLIC_TRANSPORT"
}
```

Required fields:

- `activityId`
- `maxMembers`
- `departureTime`
- `transportMode`

Returns `TravelGroupDto`.

### List Travel Groups

```http
GET /api/travelgroups
```

Returns all travel groups whose activities are visible to the current user.

### Get Route Suggestions

```http
GET /api/travelgroups/{groupId}/route-suggestions
```

Query parameters:

| Name | Type | Default | Notes |
| --- | --- | --- | --- |
| `maxResults` | int | `4` | Must be between 1 and 25 |
| `originLatitude` | double | saved group origin | Optional override |
| `originLongitude` | double | saved group origin | Optional override |
| `originLabel` | string | saved group origin label | Optional override |
| `destinationLatitude` | double | activity destination | Optional override |
| `destinationLongitude` | double | activity destination | Optional override |
| `destinationLabel` | string | activity location | Optional override |
| `departureTime` | ISO date-time | group departure time | Optional override |

Returns `TravelGroupRouteSuggestionsDto`.

### Join Travel Group

```http
POST /api/travelgroups/{groupId}/join
```

If join approval is disabled, the current user joins directly. If join approval is enabled, this creates a pending join request.

Returns:

```json
{
  "memberCount": 3,
  "maxMembers": 4,
  "joined": true,
  "pendingApproval": false,
  "message": "..."
}
```

### Accept Join Request

```http
POST /api/travelgroups/requests/{requestId}/accept
```

Owner-only workflow. Adds the requester to the group if capacity is still available.

### Reject Join Request

```http
POST /api/travelgroups/requests/{requestId}/reject
```

Owner-only workflow. Marks the request as rejected.

### Transfer Ownership

```http
POST /api/travelgroups/{groupId}/ownership?newOwnerId={memberId}
```

Transfers ownership to an existing member of the group.

Returns `TravelGroupDto`.

### Leave Travel Group

```http
DELETE /api/travelgroups/{groupId}/leave
```

The owner cannot leave until ownership is transferred.

### Delete Travel Group

```http
DELETE /api/travelgroups/{groupId}
```

Owner-only workflow. Deletion is allowed only when the owner is the only member.

## De Lijn API

Base path: `/api/delijn`

These endpoints require `DELIJN_API_KEY` unless they are explicit debug helpers that can return configuration diagnostics.

### Nearby Stops

```http
GET /api/delijn/nearby-stops?lat=51.2172&lng=4.4211&radiusMeters=1000&maxResults=5
```

Rules:

- `lat` must be between -90 and 90.
- `lng` must be between -180 and 180.
- `radiusMeters` must be between 1 and 5000.
- `maxResults` must be between 1 and 25.

Returns a list of `DeLijnStopDto`.

### Nearby Stops Debug

```http
GET /api/delijn/nearby-stops/debug?lat=51.2172&lng=4.4211&radiusMeters=1000
```

Returns `DeLijnDebugDto`.

### Stop Suggestions

```http
GET /api/delijn/stop-suggestions?q=Antwerp&lat=51.2172&lng=4.4211&maxResults=15
```

Rules:

- Query shorter than 2 characters returns an empty list.
- Coordinates are optional, but if one coordinate is supplied both must be valid.
- `maxResults` must be between 1 and 25.

Returns a list of `DeLijnStopDto`.

### Scheduled Departures

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/scheduled-departures?date=2026-07-09&departureTime=2026-07-09T13:10:00&maxResults=8
```

Returns a list of `DeLijnDepartureDto`.

### Stop Details

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}
```

Returns `DeLijnStopDto`.

### Stop Details Debug

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/debug
```

Returns `DeLijnDebugDto`.

### Probe Stop Endpoints

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/probe?maxResults=4
```

Returns a list of `DeLijnDebugDto`.

### Real-Time Departures

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/departures?maxResults=4
```

Returns a list of `DeLijnDepartureDto`.

### Real-Time Departures Debug

```http
GET /api/delijn/stops/{entityNumber}/{stopNumber}/departures/debug?maxResults=4
```

Returns `DeLijnDebugDto`.

## Member API

Base path: `/api/members`

### List Members

```http
GET /api/members
```

Returns all members.

### Get Member By Original Id

```http
GET /api/members/{originalId}
```

Example original id:

```text
GOOGLE-108607539392394218230
```

Returns a `Member`.

### Update Current Member Travel Preferences

```http
PUT /api/members/me/travel-preferences
```

Request body:

```json
{
  "preferredTransportMode": "PUBLIC_TRANSPORT",
  "defaultDepartureLocation": "Antwerp Central Station",
  "defaultLatitude": 51.2172,
  "defaultLongitude": 4.4211
}
```

Rules:

- `preferredTransportMode` is required.
- Latitude and longitude must be supplied together.
- Latitude must be between -90 and 90.
- Longitude must be between -180 and 180.

Returns `MemberTravelPreferenceDto`.

## Notification API

Base path: `/api/notifications`

### List Notifications

```http
GET /api/notifications
```

Returns the latest 20 notifications for the current user as `NotificationDto`.

### Get Unread Count

```http
GET /api/notifications/unread-count
```

Returns:

```json
{
  "count": 3
}
```

### Mark One Notification Read

```http
PUT /api/notifications/{id}/read
```

Only marks the notification if it belongs to the current user.

### Mark All Notifications Read

```http
PUT /api/notifications/read-all
```

Marks all unread notifications for the current user as read.

## Message Report API

Base path: `/api/messages`

### Report A Message

```http
POST /api/messages/{messageId}/reports
```

Request body:

```json
{
  "reason": "Inappropriate message"
}
```

Rules:

- Reason is required.
- A user cannot report their own message.
- The same user cannot report the same message twice.

Returns `201 Created` with a `Location` header.

## Moderator API

Base path: `/api/moderators`

Class-level access:

```java
@PreAuthorize("hasRole('MODERATOR')")
```

### Get Report Detail

```http
GET /api/moderators/reports/{reportId}
```

Returns `ReportDetailDto`.

### Get Report Chat Context

```http
GET /api/moderators/reports/{reportId}/context
```

Returns the flagged message plus up to five messages before and after it.

### Get Report History

```http
GET /api/moderators/reports/{reportId}/history
```

Returns a list of `ReportHistoryEntryDto`.

### List Reports

```http
GET /api/moderators/reports?status=OPEN&pending=false
```

Parameters:

| Name | Type | Notes |
| --- | --- | --- |
| `status` | `ReportStatus` | Optional status filter |
| `pending` | boolean | When true, returns `OPEN` and `REVIEWED` reports |

Returns a list of `ReportDto`.

### Claim Report

```http
POST /api/moderators/reports/{reportId}/claim
```

Assigns the report to the current moderator. If the report is `OPEN`, it becomes `REVIEWED`.

### Update Report Status

```http
PATCH /api/moderators/reports/{reportId}/status?status=RESOLVED
```

Only the assigned moderator can update an already assigned report.

### List Moderators

```http
GET /api/moderators/list
```

Requires:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Returns a list of `ModeratorDto`.

### Assign Report

```http
POST /api/moderators/reports/{reportId}/assign?moderatorId={moderatorId}
```

Requires:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Assigns a report to a selected moderator.

### List All Report History

```http
GET /api/moderators/history
```

Returns all report history entries ordered newest first.

## Super Admin API

Base path: `/super_admin/api`

### List Users

```http
GET /super_admin/api/user?name=yoran&role=SUPER_ADMIN
```

Requires:

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

Returns a list of `UserView`.

### List Activities

```http
GET /super_admin/api/activities?search=workshop&timing=UPCOMING
```

Requires:

```java
@PreAuthorize("hasRole('MODERATOR')")
```

Optional `timing` values:

- `UPCOMING`
- `PAST`

Returns a list of `ActivityView`.

### Update Activity Verification

```http
PATCH /super_admin/api/activities/{id}/verification?status=APPROVED
```

Requires:

```java
@PreAuthorize("hasRole('MODERATOR')")
```

`status` must be `APPROVED` or `DISAPPROVED`.

### Delete Activity

```http
DELETE /super_admin/api/activities/{id}
```

Requires:

```java
@PreAuthorize("hasRole('MODERATOR')")
```

Deleting an activity also deletes related travel groups.

### Update User

```http
PUT /super_admin/api/user/{id}
```

Requires:

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

Request body:

```json
{
  "status": true,
  "role": "ADMIN"
}
```

Allowed roles:

- `MEMBER`
- `MODERATOR`
- `ADMIN`
- `SUPER_ADMIN`

The service prevents removing the last super admin.

## Activity API

Base path: `/api/activities`

### Create Activity

```http
POST /api/activities
```

Request body maps directly to the `Activity` entity. The service sets the creator to the current user and sets verification status to `PENDING`.

Returns the saved `Activity`.

## Session API

### Keep Session Alive

```http
GET /session/keep-alive
```

Returns `204 No Content`. Used by frontend session handling.

