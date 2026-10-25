import $ from "jquery";
import { getCsrfToken } from "./csrf";

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
