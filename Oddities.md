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
- `BugReportService.create` with an assignee leaves the status `OPEN` and publishes no event. `updateAssignee` also leaves the status unchanged (never sets `ASSIGNED`), but it does publish an event.
- `updateStatus` publishes a notification even when the status did not change. Besides rejecting `CLOSED`, no transition rules are checked.
- `updateComponent` does not check that the component belongs to the report's project.
- `getDevelopers` loads all accounts and filters them in memory.
- The `archived_at` column exists on `user_account`, `software_project` and `component`, but no entity, mapper or query uses it. Archiving does not exist in the code.
- `BugReport`'s builder always starts a report as `OPEN` and cannot set id, status or resolution; `BugReportMapper.toDomain` sets them afterwards.

### Security and access
- `SecurityConfig` says comment removal is "an administrative action", but the URL rule only requires being signed in. The service then allows the ADMIN role or the comment's author.
- `GET /api/reports` shows all reports to every signed-in user (no filtering by owner).
- CORS allows only `http://localhost:5173` (hard-coded).
- `CurrentUserAuthenticationFilter` looks the user up in the database on every request.
- Demo passwords are hard-coded in `BugReportApplication` and are written to the application log at startup (only BCrypt hashes are stored).
- `application.yml` contains database, Artemis and mail credentials in plain text (dev defaults).
- `ApiExceptionHandler` handles only a fixed list of exceptions. Unexpected errors, 405 and unsupported media type errors fall back to Spring's default handling (not checked).

### API design
- `BugReportResponse` serializes the domain `Resolution` object directly, tying the API shape to the domain class.
- `UserAccountResponse.username` holds the display name; the login name is `email`.
- `BugReportBriefResponse.createdAt` is a pre-formatted string (`dd-MM-yyyy HH:mm`), while every other timestamp is ISO-8601.
- DTO naming is inconsistent: `id` / `userId` / `reportId`, and `username` / `name` / `author`.
- `PATCH /api/accounts/{id}/role` returns 204, while the other update endpoints return 200 with a body.
- The component update path segment is camelCase (`/responsibleUserId`), unlike the kebab-case report segments (`/steps-to-reproduce`).

### Messaging
- The sender address `no-reply@bugreport.local` is hard-coded in `BugReportEventConsumer`.
- `BugReportEventPublisher` does not catch send failures; the database change is already committed when sending fails.

### Small things
- The comment "These accounts exist only in the in-memory demo database" in `BugReportApplication` does not match the configuration: both the application and the tests use MariaDB.
- `Attachment` exists only in the domain module (no entity, repository or endpoint); its demo seeding code is commented out.
- `findAllByRole` in `UserAccountRepositoryJpaAdapter` is a normal transaction while the rest of the class is read-only, which is needed for its pessimistic lock.
- No JPA associations are mapped except `BugReportEntity.resolution`; the other relations are plain UUID columns with foreign keys only in `V1__Base.sql`.

## Frontend

- jQuery (`$.ajax`) is used for all requests although `fetch` is available.
- The api files differ in style (quoted `"url"` keys, missing semicolons in some).
- `api/create-bug-report-options.js` duplicates `getAllProjects` and `getAllComponents`.
- A failed login is only written to the browser console; the user sees nothing.
- The "Register" button on the login form does nothing.
- `ProtectedRoute` renders nothing while the session check is running.
- `Modal` relies on Bootstrap's own JavaScript instead of React state.
- `Toast` uses a hard-coded 5-second timeout.
- `BugReportPage`: adding a comment does not check for blank text in the UI; the backend rejects it and only a generic error toast appears.
- `BugReportPage`: changing the assignee in the UI does not change the status (same as the backend).
- The admin pages `ProjectAdminPage` and `ComponentAdminPage` duplicate the inline `EditableName` component and the description-modal logic.
- `ProjectAdminPage` reads `auth.currentUser.role` without a null check; this is only safe because `ProtectedRoute` guards the page.
- `UserAdminPage` lets an admin start editing their own role, although the backend refuses it (only a generic toast is shown). It also loads all users and filters and pages them in the browser.
- The description modals on the admin pages have no close (X) button in the header.
- `HomePage` and `BugReportPage` load the developers/projects/components lists on mount even if the create form or the select fields are never used.
- The pages use `document.getElementById` together with Bootstrap's `Modal` API to open and close modals, mixing DOM access with React state.

## Scripts and infrastructure

- `run-backend.sh` called with two or more arguments prints a message but exits with status 0 (a bare `exit`), so it looks like success. An unknown single argument is ignored silently.
- The Vite proxy target `http://backend:8080` (`frontend/vite.config.js`) only resolves inside the compose network, so `npm run dev` on the host has no working backend.
- `docker-compose.yml` uses the `latest` tag for `mariadb` and `apache/activemq-artemis`, so versions are not pinned.
- `backend/Dockerfile` hard-codes the jar name `bug-report-api-0.1.0-SNAPSHOT.jar`; changing the version in the pom breaks the image build.
- `database/init/01-create-test-database.sql` only runs on an empty database volume, so an existing volume never gets the `bug_report_test` database.
