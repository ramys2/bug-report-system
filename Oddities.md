# Oddities

Things noticed while adding documentation comments (branch `docs/code-comments`).
Nothing here was changed: this is a report only. Items marked **TODO(verify)** are also
marked in the code comments and were not confirmed by running the application.

## Open questions (TODO(verify))

| # | Where | Question |
|---|---|---|
| 2 | `BugReportAuthorizer`, `CommentAuthorizer` | They throw `ResourceNotFoundException` while a `@PreAuthorize` expression is evaluated. Does that reach the client as 404, or is it wrapped into another error (e.g. 500)? |
| 4 | `run-backend.sh` | `application.yml` uses the hosts `database`, `artemis` and `mailpit` (compose service names). How do they resolve when the backend runs directly on the host? |

## Backend

### Behavior and business rules
- The `archived_at` column exists on `user_account`, `software_project` and `component`, but no entity, mapper or query uses it. Archiving does not exist in the code.
- `BugReport`'s builder always starts a report as `OPEN` and cannot set id, status or resolution; `BugReportMapper.toDomain` sets them afterwards.

### Security and access
- `GET /api/reports` shows all reports to every signed-in user (no filtering by owner).
- `CurrentUserAuthenticationFilter` looks the user up in the database on every request.
- Demo passwords are hard-coded in `BugReportApplication` and are written to the application log at startup (only BCrypt hashes are stored).
- `application.yml` contains database, Artemis and mail credentials in plain text (dev defaults).
- `ApiExceptionHandler` handles only a fixed list of exceptions. Unexpected errors, 405 and unsupported media type errors fall back to Spring's default handling (not checked).

### API design
- `BugReportResponse` serializes the domain `Resolution` object directly, tying the API shape to the domain class.
- The component update path segment is camelCase (`/responsibleUserId`), unlike the kebab-case report segments (`/steps-to-reproduce`).

### Messaging
- `BugReportEventPublisher` does not catch send failures; the database change is already committed when sending fails.

### Small things
- `Attachment` exists only in the domain module (no entity, repository or endpoint); its demo seeding code is commented out.
- `findAllByRole` in `UserAccountRepositoryJpaAdapter` is a normal transaction while the rest of the class is read-only, which is needed for its pessimistic lock. `findByRole` runs the same query without a lock, and the two names look alike (Spring Data treats `findBy` and `findAllBy` the same), so use `findByRole` for plain reads and `findAllByRole` only where the lock is wanted (`updateRole`).
- No JPA associations are mapped except `BugReportEntity.resolution`; the other relations are plain UUID columns with foreign keys only in `V1__Base.sql`.

## Frontend

- jQuery (`$.ajax`) is used for all requests although `fetch` is available.
- The api files differ in style (quoted `"url"` keys, missing semicolons in some).
- `api/create-bug-report-options.js` duplicates `getAllProjects`.
- A failed login is only written to the browser console; the user sees nothing.
- The "Register" button on the login form does nothing.
- `Modal` relies on Bootstrap's own JavaScript instead of React state.
- `Toast` uses a hard-coded 5-second timeout.
- `HomePage` and `BugReportPage` load the developers and projects lists on mount even if the create form or the select fields are never used.

## Scripts and infrastructure

- `run-backend.sh` called with two or more arguments prints a message but exits with status 0 (a bare `exit`), so it looks like success. An unknown single argument is ignored silently.
- The Vite proxy target `http://backend:8080` (`frontend/vite.config.js`) only resolves inside the compose network, so `npm run dev` on the host has no working backend.
