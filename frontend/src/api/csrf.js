import $ from "jquery"

/**
 * Cached CSRF token value, or `null` if none is loaded.
 */
let csrfToken = null;
/**
 * Name of the header the token must be sent in (`X-CSRF-TOKEN`), or `null` if none is loaded.
 */
let csrfHeaderName = null;

/**
 * Fetches a CSRF token from `GET /api/csrf` and caches it in this module.
 * Must complete before any POST/PATCH/DELETE request is sent; the state-changing api functions read the cache via `getCsrfToken()`.
 *
 * @returns {JQuery.Promise<void>} resolves once the token is cached
 */
export function loadCsrfToken() {
    return $.ajax({
        method: "GET",
        url: "/api/csrf"
    }).then((data) => {
        csrfToken = data.token;
        csrfHeaderName = data.headerName;
    });
}

/**
 * Returns the cached CSRF token.
 *
 * @returns {{token: string|null, headerName: string|null}} both fields are `null` until `loadCsrfToken()` has completed
 */
export function getCsrfToken() {
    return {
        token: csrfToken,
        headerName: csrfHeaderName
    }
}

/**
 * Forgets the cached token (called after logout).
 */
export function cleanCsrfToken() {
    csrfToken = null;
    csrfHeaderName = null;
}
