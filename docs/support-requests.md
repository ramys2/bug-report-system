# Support requests

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

## Administrator API

`GET /api/support` is Administrator-only and returns all requests, newest first.
Each item includes `id`, `requesterId`, `requesterName`, `type`, `requestedRole`,
`description`, `status`, `createdAt`, and `resolvedAt`. Requester names are loaded
from existing accounts in one batch. This simple listing is not paginated.

`PATCH /api/support/{id}/status` is Administrator-only and requires a CSRF token:

```json
{"status":"APPROVED"}
```

Use `REJECTED` to reject a request. `PENDING` is not a resolution. Successful
resolution returns HTTP 200 with only:

```json
{"code":"SUPPORT_REQUEST_RESOLVED","message":"Support request resolved successfully."}
```

The service locks the request before checking its pending status. For a role-change
approval it also locks and reloads the requester, revalidates Reporter to Developer,
and calls the existing `UserAccountService.updateRole()`. The role update, resolved
status, and server-supplied resolution timestamp are committed in one transaction.
If either update fails, both changes roll back. Concurrent decisions can resolve
a request only once. Administrators cannot approve their own historical requests.

Outdated role changes return a conflict and remain pending, so an administrator can
inspect or reject them. Rejection and approval of `OTHER` never modify user roles.
Resolution uses the columns already present in V6; no additional migration is needed.

| Status | Meaning |
| --- | --- |
| 404 `SUPPORT_REQUEST_NOT_FOUND` | The request does not exist |
| 409 `SUPPORT_REQUEST_ALREADY_RESOLVED` | Another decision has already resolved the request |
| 409 `SUPPORT_INVALID_RESOLUTION_STATUS` | The supplied resolution is not APPROVED or REJECTED |
| 409 `SUPPORT_ROLE_CHANGE_OUTDATED` | The target role is no longer eligible for the current account |

Missing/unknown status values return 400. Unauthenticated users receive 401 when
CSRF is valid, and non-administrators receive 403. Missing/invalid CSRF and self-approval
also receive 403. Management permissions are checked both in HTTP security and on services.

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

Fetch a fresh CSRF token after each login/logout.

### Administrator page

1. Create a role-change request and a general request as a Reporter using the Navbar
   dialog. Sign in as an Administrator and select **Support requests**, opening
   `/admin/support`.
2. Confirm the table shows requester name/id, request type, requested role and full
   description where provided, status, creation date, and resolution date. Requests
   are newest first. The page lists all requests returned by the existing API.
3. Approve a pending Developer request. Expect a success notification, a refreshed
   row marked Approved with a resolution date, and a Developer role on the existing
   user administration page. Reject another role request and confirm its user's
   role is unchanged. General requests should resolve without changing roles.
4. Resolved rows must have no action buttons. An Administrator's own historical
   pending request has Approve disabled with an explanation. During resolution,
   action buttons and Refresh are disabled to prevent repeated submissions.
5. Open the page in two Administrator tabs. Resolve a request in one tab, then try
   resolving its stale row in the other. Expect a localized conflict message and
   a refreshed list showing the resolved status.
6. Change a requester's role through existing user administration before approving
   their request. Expect an outdated-role error; the request stays pending and can
   still be rejected.
7. Check labels, types, statuses, and feedback in English and Czech. Check the empty
   state before requests exist, and use Refresh to recover after a failed load.
8. Reporters and Developers must not see the management link and must be redirected
   when visiting `/admin/support`. Signed-out visitors are redirected to login.

### Administrator API checks

Sign in as an Administrator and use the frontend browser console to inspect requests:

```js
const supportRequests = await fetch("/api/support").then(response => response.json());
console.table(supportRequests);

async function resolveSupportRequest(id, status) {
    const csrf = await fetch("/api/csrf").then(response => response.json());
    const response = await fetch(`/api/support/${id}/status`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({ status }),
    });
    console.log(response.status, await response.json());
}
```

1. Call `resolveSupportRequest("<pending-request-id>", "APPROVED")` for a Reporter's
   Developer request. Expect HTTP 200, the two-field confirmation, an approved request
   with `resolved_at`, and Developer in the corresponding `user_account` row.
2. Repeat resolution of that id: expect 409 and an unchanged resolution timestamp.
3. Reject a different pending role request: expect HTTP 200, a rejected status, and
   an unchanged account role. The Reporter can then submit a new request for Developer.
4. Approve/reject general requests: only their status and resolution timestamp change.
5. Before approving another role request, change its requester to Developer or Administrator
   through existing user administration. Approval should return 409 and keep the request pending;
   rejecting it should still work.
6. List/resolve as Reporter or Developer: expect 403. Signed out: expect 401 with a valid
   CSRF token. Omit CSRF during a signed-in resolution: expect 403.

The backend integration tests also cover competing approval/rejection and rollback
after both updates have been flushed.
