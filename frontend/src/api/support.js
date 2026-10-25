import $ from "jquery";
import { getCsrfToken } from "./csrf";

/**
 * Lists support requests, newest first, with requester details. `GET /api/support` (ADMIN only).
 *
 * @returns {JQuery.jqXHR} resolves with request id, requesterId/name, type, requestedRole,
 * description, status, createdAt and resolvedAt for each request
 */
export function getSupportRequests() {
    return $.ajax({
        method: "GET",
        url: "/api/support",
    });
}

/**
 * Creates a support request with `POST /api/support` as the signed-in Reporter or Developer.
 * Sends the cached CSRF token loaded by AuthProvider/login. Identity and status are set by the backend.
 *
 * @param {{type: "ROLE_CHANGE"|"OTHER", requestedRole?: "DEVELOPER", description?: string}} request
 * @returns {JQuery.jqXHR} resolves (201) with `{ code, message }`
 */
export function createSupportRequest({ type, requestedRole, description }) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: "/api/support",
        contentType: "application/json",
        data: JSON.stringify({ type, requestedRole, description }),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}

/**
 * Resolves a pending support request. `PATCH /api/support/{id}/status` (ADMIN only).
 * Uses the cached CSRF token. The backend checks eligibility and prevents repeated resolution.
 *
 * @param {string} requestId
 * @param {"APPROVED"|"REJECTED"} status resolution to apply
 * @returns {JQuery.jqXHR} resolves with `{ code, message }`
 */
export function updateSupportRequestStatus(requestId, status) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/support/${requestId}/status`,
        contentType: "application/json",
        data: JSON.stringify({ status }),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
