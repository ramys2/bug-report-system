# Support-request creation

`POST /api/support` creates a persisted request for the signed-in Reporter or Developer.
The server supplies the requester, generated id, `PENDING` status and creation timestamp.
Creating a request never changes an account's role.

## Request bodies

A Reporter can request Developer access:

```json
{"type":"ROLE_CHANGE","requestedRole":"DEVELOPER"}
```

Reporters and Developers can ask for other assistance:

```json
{"type":"OTHER","description":"Please help me find the correct project."}
```

`ROLE_CHANGE` requires a target role. Only Reporter to Developer is eligible in the
existing hierarchy. The description is optional for role changes. `OTHER` requires
a nonblank description and must omit `requestedRole` (or use `null`). Descriptions
allow at most 10,000 characters and are trimmed before storage.

Successful creation returns HTTP 201 with only a stable code and confirmation message:

```json
{"code":"SUPPORT_REQUEST_CREATED","message":"Support request created successfully."}
```

The saved request has no resolution timestamp. Submitted identity, status and timestamp
fields are ignored by the creation DTO.

## Persistence and security

Flyway `V6__Support_request.sql` creates `support_request` with a foreign key to
`user_account`, checks for valid types/statuses/details, and a unique key on
`(requester_id, pending_requested_role)`. MariaDB computes `pending_requested_role`
only for pending role changes. Other rows use `NULL`, so general requests and
resolved history do not block new requests. The generated column is internal to
the database and is neither accepted nor returned by the API.

Both HTTP security and the service restrict creation to non-administrators. The
service obtains identity from the security context and reloads the stored account
to validate its current role. Session authentication and CSRF protection remain enabled.
Logs include ids, type and requested role; they omit descriptions and credentials.

Errors use the existing JSON error format:

| Status | Meaning |
| --- | --- |
| 201 `SUPPORT_REQUEST_CREATED` | Request saved |
| 400 `INVALID_REQUEST` | Missing/invalid type, required fields, or description length |
| 401 `AUTHENTICATION_REQUIRED` | No authenticated session, when a valid CSRF token is supplied |
| 403 `ACCESS_DENIED` | Administrator, invalid application principal, or missing/invalid CSRF token |
| 409 `SUPPORT_ROLE_NOT_ALLOWED` | Current, lower, or Administrator role requested |
| 409 `SUPPORT_REQUEST_ALREADY_PENDING` | A pending request for the same role already exists |
| 409 `DATA_CONFLICT` | A database constraint rejects an insert, including a racing duplicate |

## Manual checks

### Navbar dialog

1. Sign in as a Reporter and select **Support** in the Navbar. Under **Request role
   change**, only Developer should be available. Select it and send the request.
   Expect a success notification, the dialog to close, and an empty form when reopened.
2. Submit the same role request again. Expect a duplicate-request error with the
   dialog left open. The backend remains responsible for rejecting duplicates.
3. Choose **Different service**. Submission must stay disabled for an empty or
   whitespace-only description. Enter a description and submit successfully.
4. Sign in as a Developer. The role-change dropdown and submission are disabled
   because no eligible higher role exists. General requests should still work.
5. Confirm the Support button is hidden for Administrators and signed-out visitors.
6. Check the labels and success/error feedback in English and Czech. While a
   request is in progress, fields and submission are disabled. Closing and reopening
   the dialog preserves the draft and any pending submission; success clears it.

### API checks

Run the existing development frontend/backend with MariaDB available. Flyway applies
V6 when the backend starts. Sign in through the frontend as a Reporter and use the
browser's developer console on that frontend page:

```js
const csrf = await fetch("/api/csrf").then(response => response.json());
const response = await fetch("/api/support", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ type: "ROLE_CHANGE", requestedRole: "DEVELOPER" }),
});
console.log(response.status, await response.json());
```

1. Expect 201 with only `code: "SUPPORT_REQUEST_CREATED"` and the confirmation message.
   Inspect `support_request` in the database: it should contain your account's
   `requester_id`, `PENDING`, a creation timestamp, and a null `resolved_at`.
   Confirm your role remains Reporter using `GET /api/auth/me`.
2. Repeat the request: expect 409 and only one pending request in the database.
3. Request `REPORTER` or `ADMIN`: expect 409. As a Developer, all role-change
   requests also return 409.
4. Submit `OTHER` with a description: expect 201. Blank/missing descriptions return 400.
5. Add another user's `requesterId`, `status: "APPROVED"`, or timestamps to a valid
   body: the saved request must still belong to you and remain pending.
6. Try as an Administrator: expect 403. Try signed out with a newly fetched CSRF
   token: expect 401. Omit the CSRF header while signed in: expect 403.

Fetch a fresh CSRF token after each login/logout. Administrator review/resolution
functionality is not implemented yet.
