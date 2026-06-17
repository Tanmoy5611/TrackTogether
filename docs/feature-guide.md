# Feature Guide

## Login and User Provisioning

Users log in with Google OAuth2/OIDC. During login:

1. Google returns an OIDC user.
2. The application builds `originalId` as `GOOGLE-` plus the Google subject.
3. The matching `Member` is loaded or created.
4. Name and email are updated from Google.
5. Authorities are added based on role tables.

Disabled users are redirected to `/banned` for browser requests. API requests receive a forbidden response.

## Home Page

The home page is personalized by `HomePageService`.

It can show:

- A welcome message.
- Latest relevant activity.
- Upcoming activities.
- Suggested activities based on same location or nearby time.
- Open travel groups the user can still join.
- Suggested travel groups from `FriendMatchingService`.
- Personal CO2 savings.

## Activities

Members can create activities. Created activities start with `PENDING` verification status.

Visibility rules:

- `APPROVED` activities are visible to everyone.
- Non-approved activities are visible to their creator.
- Activity lists and travel group lists use `ActivityPolicyService` so hidden activities do not leak through related travel groups.

Moderators can verify activities through super admin activity APIs and pages.

Deleting an activity also deletes related travel groups through `TravelGroupService.deleteTravelGroupsForActivity`.

## Travel Groups

Travel groups are the central workflow.

Members can:

- Create a travel group for an activity.
- Edit their owned travel group.
- Join or request to join.
- Leave joined groups.
- Transfer ownership.
- Delete a group when they are the owner and only member.
- Invite another member by email.
- Share, update, or clear their location in a group.
- Open the linked group chat.
- View route suggestions for public transport groups.
- View the activity log for a group.

Important rules:

- Travel group owners are automatically added as members.
- Only owners can edit, delete, accept/reject requests, and transfer ownership.
- Owners cannot leave their own group until ownership is transferred.
- Owners can delete only when they are the only member.
- Maximum members cannot be lower than the current member count.
- Departure time must not be in the past.
- Departure time must be before the activity starts when the activity date/time is known.
- Estimated arrival must not be before departure or after the activity starts.
- Departure coordinates must be supplied as a pair and must be valid latitude/longitude values.

### Join Approval

The singleton `SystemSettings.travelGroupJoinApprovalEnabled` controls join behavior.

When disabled:

- Members join directly if space is available.
- The group owner receives a join notification.
- If the group becomes full, all group members receive a group-full notification.

When enabled:

- Joining creates a `PENDING` `JoinRequest`.
- The group owner receives a join request notification.
- The owner can accept or reject.
- Accepted requests add the member to the group and group chat.
- Rejected requests notify the requester.

### Concurrency Protection

Seat-sensitive travel group actions load the group with a pessimistic database lock. This prevents two concurrent requests from both taking the last available seat.

## Travel Group Activity Log

`TravelGroupActivityLog` records major group events:

- Creation
- Join
- Leave
- Update
- Ownership transfer
- Join request created
- Join request accepted
- Join request rejected

The detail and activity log pages use this history for visibility into group changes.

## Location Sharing

Travel group members can share a location with:

- Address
- Latitude
- Longitude
- Timestamp

Each membership can have at most one `Location`. Re-sharing updates the same row. Clearing the location removes it from the membership.

## Friend and Travel Group Matching

`FriendMatchingService` scores candidate travel groups for the current user.

Candidates are excluded when:

- The group is already joined by the user.
- The group is owned by the user.
- The activity is not visible.
- The group is full.
- The group is in the past.
- The user already has a pending join request.

Scoring can consider:

- Same activity.
- Requested or preferred transport mode.
- Compatible transport modes.
- Nearby departure coordinates.
- Matching departure location text.
- Similar departure time.
- Public transport compatibility through De Lijn route data.

Public transport matching can compare:

- Same origin stop.
- Shared line numbers.
- Arrival before the event.
- Similar route departure times.

## De Lijn Route Suggestions

De Lijn support is used for public transport workflows.

Capabilities:

- Nearby stop lookup.
- Stop search suggestions.
- Stop details.
- Real-time departures.
- Scheduled departures.
- Route suggestions for public transport travel groups.
- Debug/probe endpoints for endpoint setup.

Route suggestions are only supported for `PUBLIC_TRANSPORT` travel groups. If coordinates or API keys are missing, the response remains user-friendly and explains what is missing.

## Chat

The app supports three conversation types:

- `DIRECT`: one-to-one member chat.
- `TRAVEL_GROUP`: chat attached to a travel group.
- `CUSTOM_GROUP`: manually created group chat.

Direct chats are created between two members.

Travel group chats:

- Are linked one-to-one with a travel group.
- Can only be opened by members of that travel group.
- Keep membership in sync when members join or leave.

Custom group chats:

- Have a title.
- Require at least one selected member at creation.
- Add the creator as `OWNER`.
- Support rename, add member, remove member, role update, and leave.
- Require an owner for management actions.
- Prevent removing or demoting the last owner.

## Message Reporting and Moderation

Members can report another member's message with a reason.

Report creation rules:

- Reason is required.
- A member cannot report their own message.
- The same reporter cannot report the same message twice.
- New reports start as `OPEN`.

Moderators can:

- View reports.
- Filter by status.
- View report detail.
- View a chat context window around the flagged message.
- Claim a report.
- Update status.
- View report history.

Admins can:

- List moderators.
- Assign a report to a specific moderator.

Report status changes are recorded in `ReportHistoryEntry`.

## Notifications

Notifications are created for:

- A member joining a group.
- A member leaving a group.
- A group becoming full.
- A join request received by an owner.
- A join request accepted.
- A join request rejected.
- A new message in a travel group chat.

The notification API returns the latest 20 notifications for the current user, unread count, and read operations.

## Analytics

Analytics are generated by `AnalyticsService`.

User analytics include:

- Trips created.
- Trips joined.
- Personal baseline emissions.
- Personal actual emissions.
- Personal savings percentage.
- Transport mode breakdown.
- Personal CO2 savings.

Admin analytics include:

- Total events.
- Total travel groups.
- Total participants.
- Average group size.
- Most popular activities.
- Transport popularity.
- Peak travel times.
- Baseline, actual, and saved CO2.
- CO2 savings per activity.
- CO2 savings per trip.
- CO2 through time by day, month, or quarter.

CO2 assumptions:

- Baseline assumes every participant travels alone by car.
- Car: `0.120 kg CO2/km/person`.
- Carpool: `0.120 kg CO2/km` for the car, shared across passengers for personal analytics.
- Public transport: `0.040 kg CO2/km/person`.
- Bike and walk: `0 kg CO2/km`.
- If an activity has no distance, the default distance is `10 km`.

## Super Admin and Admin Workflows

Super admin capabilities:

- List users.
- Filter users by name and role.
- View user details.
- Enable or disable accounts.
- Change user roles.
- Preserve at least one super admin.
- Expire user sessions after status or role changes.

Moderator/admin activity capabilities:

- List activities.
- Filter by search and timing.
- Approve or disapprove activities.
- Delete activities.

System settings:

- Toggle whether travel group joins require owner approval.

