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

### Security and access
- `GET /api/reports` shows all reports to every signed-in user (no filtering by owner).
- **Accepted for now:** `CurrentUserAuthenticationFilter` looks the signed-in user up in the database on every request so role changes apply on the next request. The extra lookup is an acceptable tradeoff for this semester project; caching is deferred unless measurements show a performance problem.
- Demo passwords are hard-coded in `BugReportApplication` and are written to the application log at startup (only BCrypt hashes are stored).
- `application.yml` contains database, Artemis and mail credentials in plain text (dev defaults).
- `ApiExceptionHandler` handles only a fixed list of exceptions. Unexpected errors, 405 and unsupported media type errors fall back to Spring's default handling (not checked).

### Messaging
- `BugReportEventPublisher` sends asynchronously after commit using a dedicated executor with one worker and a queue of 100 tasks by default. Configure it through `messaging.publisher.core-pool-size`, `max-pool-size`, and `queue-capacity`; invalid values prevent startup. It catches JMS send failures and makes up to three attempts, one second apart. Each outcome is logged to `logs/bug-report-publisher.log` (directory configurable with `PUBLISHER_LOG_PATH`), with rotation at 10 MB or daily, seven days of history and a 100 MB archive cap. Serialization failures are logged without retrying. Full queues or executor shutdown cause new submissions to be logged and dropped, never executed on the request thread. The database change is already committed; exhausted retries lose the notification, pending tasks do not survive a restart, and uncertain send results can cause duplicate notifications.

### Small things
- `Attachment` exists only in the domain module (no entity, repository or endpoint); its demo seeding code is commented out.
- `findAllByRole` in `UserAccountRepositoryJpaAdapter` is a normal transaction while the rest of the class is read-only, which is needed for its pessimistic lock. `findByRole` runs the same query without a lock, and the two names look alike (Spring Data treats `findBy` and `findAllBy` the same), so use `findByRole` for plain reads and `findAllByRole` only where the lock is wanted (`updateRole`).
- No JPA associations are mapped except `BugReportEntity.resolution`; the other relations are plain UUID columns with foreign keys only in `V1__Base.sql`.

## Frontend

- jQuery (`$.ajax`) is used for all requests although `fetch` is available.
- The "Register" button on the login form does nothing.
- `Modal` relies on Bootstrap's own JavaScript instead of React state.
- `Toast` uses a hard-coded 5-second timeout.
- `HomePage` and `BugReportPage` load the developers and projects lists on mount even if the create form or the select fields are never used.

## Scripts and infrastructure

- `run-backend.sh` called with two or more arguments prints a message but exits with status 0 (a bare `exit`), so it looks like success. An unknown single argument is ignored silently.
- The Vite proxy target `http://backend:8080` (`frontend/vite.config.js`) only resolves inside the compose network, so `npm run dev` on the host has no working backend.
