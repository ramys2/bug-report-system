import $ from "jquery";
import { getCsrfToken } from "./csrf";

/**
 * `GET /api/accounts` (ADMIN only): lists one page of accounts, ordered by name. Empty filters are ignored by the backend.
 *
 * @param {object} query
 * @param {string} query.id text the account id must contain, ignoring case
 * @param {string} query.name text the name must contain, ignoring case
 * @param {string} query.email text the email address must contain, ignoring case
 * @param {string} query.role exact role (`ADMIN`, `DEVELOPER` or `REPORTER`), or `""` for any
 * @param {number} query.page zero-based page number
 * @param {number} query.size accounts per page (the backend allows 1 to 100)
 * @returns {JQuery.jqXHR} resolves with `{ items, page, size, totalElements, totalPages }`, the items being `{ id, name, email, role }`
 */
export function getAccounts({ id, name, email, role, page, size }) {
    return $.ajax({
        method: "GET",
        url: "/api/accounts",
        data: { id, name, email, role, page, size },
    });
}

/**
 * `GET /api/accounts/users?search=...` (ADMIN or DEVELOPER): finds users by name, ignoring case.
 *
 * @param {string} search text to look for
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, name }`; empty for a blank search
 */
export function searchUsers(search) {
    return $.ajax({
        method: "GET",
        url: "/api/accounts/users",
        data: { search },
    });
}

/**
 * `GET /api/accounts/developers`: users that can be chosen as assignee.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, name }`
 */
export function getDevelopers() {
    return $.ajax({
        method: "GET",
        url: "/api/accounts/developers",
    });
}

/**
 * `PATCH /api/accounts/{userId}/role` (ADMIN only): changes a user's role. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} userId id of the account
 * @param {"REPORTER"|"DEVELOPER"|"ADMIN"} role the new role
 * @returns {JQuery.jqXHR} resolves with `{id, message}`; rejects with 409 if this would remove the last admin or affect a developer with open assignments
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
