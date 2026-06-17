# Security and Roles

## Authentication

TrackTogether uses Spring Security with Google OAuth2/OIDC login.

Important classes:

| Class | Responsibility |
| --- | --- |
| `SecurityConfig` | Main security filter chain |
| `ProvisioningUserService` | Loads Google user info, creates or updates `Member`, adds authorities |
| `GoogleCloudOrganizationDomainAuthorizationRequestResolver` | Adds the `hd=kdg.be` authorization request parameter |
| `CurrentUserService` | Resolves the authenticated application `Member` |
| `AccountStatusAccessFilter` | Blocks disabled accounts |
| `UserSessionService` | Expires sessions after status or role changes |

## Public Routes

The security config permits:

- `/`
- `/css/**`
- `/js/**`
- `/images/**`
- `/favicon.ico`
- `/webjars/**`

All other routes require authentication unless covered by Spring Security login/logout internals.

## Login Flow

1. The user opens a protected page.
2. Spring Security redirects to Google login.
3. The app sends `hd=kdg.be` as a hosted-domain hint.
4. Google returns an OIDC user.
5. `ProvisioningUserService` builds an application `originalId`.
6. Existing member is loaded or a new `Member` is created.
7. Name and email are updated.
8. Authorities are assigned based on role tables.
9. The user is redirected to `/`.

## Roles

Application role data is stored in tables:

- `moderator`
- `admin`
- `super_admin`

Spring Security authorities are assigned during login:

| Database state | Granted authorities |
| --- | --- |
| Any member | `ROLE_USER` |
| Moderator | `ROLE_USER`, `ROLE_MODERATOR` |
| Admin | `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN` |
| Super admin | `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` |

## Route Protection

Global route rules:

- `/moderator/**` requires `ROLE_MODERATOR`.
- `/api/moderators/**` requires `ROLE_MODERATOR`.
- Everything else is authenticated by default.

Method-level security is enabled with `@EnableMethodSecurity`.

Important `@PreAuthorize` rules:

| Route or Controller | Rule |
| --- | --- |
| `ModeratorController` | `hasRole('MODERATOR')` |
| `ModeratorApiController` | `hasRole('MODERATOR')` |
| `GET /api/moderators/list` | `hasRole('ADMIN')` |
| `POST /api/moderators/reports/{reportId}/assign` | `hasRole('ADMIN')` |
| `GET /admin/dashboard` | `hasAnyRole('ADMIN', 'SUPER_ADMIN')` |
| `GET /analytics/dashboard` | `hasAnyRole('ADMIN', 'SUPER_ADMIN')` |
| `GET /analytics/dashboard/co2` | `hasAnyRole('ADMIN', 'SUPER_ADMIN')` |
| `GET /super_admin/home` | `hasAnyRole('ADMIN', 'SUPER_ADMIN')` |
| `GET /super_admin/user` | `hasRole('SUPER_ADMIN')` |
| `GET /super_admin/settings` | `hasRole('SUPER_ADMIN')` |
| `POST /super_admin/settings/travelgroup-approval` | `hasRole('SUPER_ADMIN')` |
| `GET /super_admin/user/{id}` | `hasRole('SUPER_ADMIN')` |
| `GET /super_admin/api/user` | `hasRole('SUPER_ADMIN')` |
| `PUT /super_admin/api/user/{id}` | `hasRole('SUPER_ADMIN')` |
| `GET /super_admin/activities` | `hasRole('MODERATOR')` |
| `GET /super_admin/api/activities` | `hasRole('MODERATOR')` |
| `PATCH /super_admin/api/activities/{id}/verification` | `hasRole('MODERATOR')` |
| `DELETE /super_admin/api/activities/{id}` | `hasRole('MODERATOR')` |

Several business rules are enforced in services rather than by route annotations. Examples:

- Only a travel group owner can manage that group.
- A member must belong to a travel group before opening its travel group chat.
- Only the custom group owner can rename or manage a custom group chat.
- The last super admin cannot be demoted or disabled through role update.

## Disabled Accounts

`AccountStatusAccessFilter` checks the current member's `status`.

If the account is disabled:

- Browser requests are redirected to `/banned`.
- API or JSON requests receive HTTP `403`.
- The disabled user can still access logout and static/login-related routes.

If an active user visits `/banned`, they are redirected back to `/`.

## Session Handling

Configuration:

- Browser inactivity timeout: `tracktogether.security.inactivity-logout-timeout-millis`
- Server session timeout: `server.servlet.session.timeout`

The default inactivity logout is 33 minutes. The server session timeout is 34 minutes.

`UserSessionService` can expire sessions for a member when:

- Account status changes.
- Role changes.

## Logout

The logout URL is:

```text
/logout
```

After logout, users are redirected to:

```text
/oauth2/authorization/google
```

The session is invalidated and `JSESSIONID` is deleted.

## CSRF

Spring Security CSRF protection is not explicitly disabled in `SecurityConfig`. Browser forms and JavaScript calls that use unsafe HTTP methods should include the CSRF token supplied by the rendered page.

## Role Management Rules

`RoleService` updates the role marker tables.

Allowed roles:

- `MEMBER`
- `MODERATOR`
- `ADMIN`
- `SUPER_ADMIN`

Role changes remove old role rows first, then add the new role rows:

- `MEMBER`: no role marker rows.
- `MODERATOR`: row in `moderator`.
- `ADMIN`: row in `admin`.
- `SUPER_ADMIN`: rows in `admin` and `super_admin`.

`SuperAdminService` prevents removing the final super admin by locking the super admin rows during the check.
