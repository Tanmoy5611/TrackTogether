# Frontend and Templates

TrackTogether uses server-rendered Thymeleaf templates with Bootstrap, Bootstrap Icons, Leaflet, and small JavaScript modules for interactive behavior.

## Template Structure

Templates live in:

```text
src/main/resources/templates
```

Shared fragments:

| Template | Purpose |
| --- | --- |
| `fragments/nav.html` | Navigation |
| `fragments/footer.html` | Footer |
| `fragments/style.html` | Shared style imports or inline shared styling |

Error templates:

| Template | Purpose |
| --- | --- |
| `error/404.html` | Not found page |
| `error/500.html` | Generic server error page |

## Page Routes

### General

| Route | Controller | Template |
| --- | --- | --- |
| `/` | `MainController` | `index` or `landing`, depending on authentication flow |
| `/banned` | `BannedController` | `banned` |
| `/session/keep-alive` | `SessionController` | No template, returns `204` |

### Activities

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /activities` | `activities` | Activity overview |
| `GET /activities/new` | `newActivity` | Create activity form |
| `POST /activities/new` | Redirect | Create activity |
| `GET /activities/{id}` | `activity-overview` | Activity detail |
| `POST /activities/{id}/delete` | Redirect | Delete activity |

### Travel Groups

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /travelgroups` | `travelgroups` | Overview of current and available groups |
| `GET /travelgroups/create` | `create-travelgroup` | Create form |
| `POST /travelgroups/create` | Redirect | Create group |
| `GET /travelgroups/{groupId}/edit` | `edit-travelgroup` | Edit form |
| `POST /travelgroups/{groupId}/edit` | Redirect | Update group |
| `POST /travelgroups/{groupId}/join` | Redirect | Join or request to join |
| `POST /travelgroups/{groupId}/leave` | Redirect | Leave group |
| `POST /travelgroups/{groupId}/delete` | Redirect | Delete owned group |
| `POST /travelgroups/{groupId}/transfer-ownership` | Redirect | Transfer ownership |
| `POST /travelgroups/{groupId}/location` | Redirect | Update shared location |
| `POST /travelgroups/{groupId}/location/live` | JSON | Update live location |
| `POST /travelgroups/{groupId}/location/clear` | Redirect | Clear shared location |
| `POST /travelgroups/{groupId}/invite` | Redirect | Invite member by email |
| `POST /travelgroups/requests/{requestId}/accept` | Redirect | Accept join request |
| `POST /travelgroups/requests/{requestId}/reject` | Redirect | Reject join request |
| `GET /travelgroups/{groupId}` | `travelgroup-detail` | Group detail |
| `GET /travelgroups/{groupId}/activity-log` | `travelgroup-activity-log` | Activity log |
| `GET /travelgroups/{groupId}/route-suggestions` | `travelgroup-route-suggestions` | De Lijn route suggestions |

### Chat

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /chat` | `chat-overview` | Chat overview |
| `POST /chat/start` | Redirect | Start direct chat |
| `GET /chat/groups/create` | `chat-overview` | Group chat creation panel |
| `POST /chat/groups/create` | Redirect | Create custom group chat |
| `POST /chat/groups/travel/{groupId}/open` | Redirect | Open travel group chat |
| `GET /chat/contacts` | `contacts` | Contact list |
| `GET /chat/{conversationId}` | `chat-overview` | Selected conversation |
| `POST /chat/{conversationId}/groups/rename` | Redirect | Rename custom group |
| `POST /chat/{conversationId}/groups/members` | Redirect | Add custom group member |
| `POST /chat/{conversationId}/groups/members/{memberId}/remove` | Redirect | Remove custom group member |
| `POST /chat/{conversationId}/groups/members/{memberId}/role` | Redirect | Update custom group member role |
| `POST /chat/{conversationId}/groups/leave` | Redirect | Leave custom group |
| `POST /chat/{conversationId}/send` | Redirect | Send message |

### Analytics

| Route | Template or Response | Purpose |
| --- | --- | --- |
| `GET /analytics/me` | `userAnalytics` | Member analytics |
| `GET /analytics/dashboard` | `analyticsDashboard` | Admin analytics dashboard |
| `GET /analytics/dashboard/co2` | JSON | CO2 through time |

### Moderator

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /moderator` | `moderator` | Moderator dashboard |
| `GET /moderator/reports/{reportId}` | `report-detail` | Report detail |

### Admin and Super Admin

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /admin/dashboard` | `superAdminDashboard` | Admin dashboard |
| `GET /admin/{id}` | `userOverview` | Member view |
| `GET /super_admin/home` | `superAdminDashboard` | Super admin dashboard |
| `GET /super_admin/user` | `allUsers` | User list |
| `GET /super_admin/activities` | `allActivities` | Activity list |
| `GET /super_admin/settings` | `system-settings` | System settings |
| `POST /super_admin/settings/travelgroup-approval` | Redirect | Toggle travel group approval |
| `GET /super_admin/user/{id}` | `userManagement` | User management |

### Member

| Route | Template | Purpose |
| --- | --- | --- |
| `GET /member/{id}` | `userOverview` | View member |
| `GET /member/profile` | `userOverview` | Current member profile |
| `POST /member/profile/travel-preferences` | Redirect | Update travel preferences |

## Static JavaScript Files

JavaScript lives in:

```text
src/main/resources/static/js
```

| File | Purpose |
| --- | --- |
| `main.js` | General UI behavior |
| `inactivity-logout.js` | Browser inactivity handling |
| `notifications.js` | Notification list/count/read behavior |
| `users.js` | User list/admin interactions |
| `user-detail.js` | User detail/admin interactions |
| `activities-admin.js` | Admin activity page behavior |
| `moderator.js` | Moderator dashboard interactions |
| `report-detail.js` | Report detail interactions |
| `chat-report.js` | Report message from chat |
| `travelgroups.js` | Travel group overview behavior |
| `travelgroup-map.js` | Map behavior for travel group detail |
| `travelgroup-location-share.js` | Shared location behavior |
| `travelgroup-delete-modal.js` | Delete confirmation modal behavior |
| `travelgroup-route-suggestions.js` | Route suggestion page behavior |
| `travelgroup-route-footer.js` | Route suggestion footer behavior |

## Styling and Assets

Main CSS:

```text
src/main/resources/static/css/main.css
```

Images:

```text
src/main/resources/static/images/logo.png
src/main/resources/static/favicon.ico
```

WebJars dependencies:

- Bootstrap `5.3.3`
- Bootstrap Icons `1.13.1`
- Leaflet `1.9.4`

## Thymeleaf Model Helpers

`GlobalControllerAdvice` adds these model attributes to pages:

| Attribute | Meaning |
| --- | --- |
| `currentUser` | Current authenticated member, or null |
| `showDashboardNav` | True for admin or super admin authority |
| `inactivityLogoutTimeoutMillis` | Browser inactivity timeout |
| `currentLanguage` | Current resolved language |
| `currentRequestUri` | Current request path |

## Localization in Views

Text should use message keys from:

- `messages.properties`
- `messages_nl.properties`

The current language is stored in the `TRACKTOGETHER_LOCALE` cookie and can be changed with the `lang` query parameter.
