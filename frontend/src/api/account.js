import $ from "jquery";
import { getCsrfToken } from "./csrf";

/**
 * `GET /api/accounts` (ADMIN only): lists all accounts.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, username, email, role }`
 */
export function getAllAccounts() {
    return $.ajax({
        method: "GET",
        url: "/api/accounts",
    });
}

/**
 * `GET /api/accounts/users?search=...` (ADMIN or DEVELOPER): finds users by name, ignoring case.
 *
 * @param {string} search text to look for
 * @returns {JQuery.jqXHR} resolves with a list of `{ userId, name }`; empty for a blank search
 */
export function searchUsers(search) {
    return $.ajax({
        method: "GET",
        url: "/api/accounts/users",
        data: { search },
    });
}

/**
 * `PATCH /api/accounts/{userId}/role` (ADMIN only): changes a user's role. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} userId id of the account
 * @param {"REPORTER"|"DEVELOPER"|"ADMIN"} role the new role
 * @returns {JQuery.jqXHR} resolves with no content (204); rejects with 409 if this would remove the last admin or affect a developer with open assignments
 */
export function updateAccountRole(userId, role) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/accounts/${userId}/role`,
        contentType: "application/json",
        data: JSON.stringify({ role }),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
