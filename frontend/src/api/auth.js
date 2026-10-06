import $ from "jquery";
import { cleanCsrfToken, getCsrfToken, loadCsrfToken } from "./csrf";

/**
 * Signs the user in: loads a CSRF token, posts the credentials to `POST /api/auth/login` (form-encoded), then loads a fresh CSRF token
 * because the session changes on login.
 *
 * @param {string} email the account's email address (sent as the `username` field)
 * @param {string} password the plain-text password
 * @returns {JQuery.Promise<void>} resolves after login; rejects with HTTP 401 for wrong credentials
 */
export function login(email, password) {
    return loadCsrfToken()
        .then(() => performLogin(email, password, getCsrfToken()))
        .then(() => loadCsrfToken());
}

/**
 * Fetches the signed-in user with `GET /api/auth/me`.
 *
 * @returns {JQuery.jqXHR} resolves with `{ id, name, email, role }`; rejects with 401 if not signed in
 */
export function getCurrentUser() {
    return $.ajax({
        method: "GET",
        url: "/api/auth/me"
    });
}

/**
 * Signs the user out with `POST /api/auth/logout` and clears the cached CSRF token.
 *
 * @returns {JQuery.Promise<void>} resolves after logout
 */
export function logout() {
    return loadCsrfToken()
        .then(() => {
            const csrfToken = getCsrfToken();

            return $.ajax({
                method: "POST",
                url: "/api/auth/logout",
                headers: {
                    [csrfToken.headerName]: csrfToken.token
                }
            });
        })
        .then(() => cleanCsrfToken());
}

/**
 * Sends the login form. `POST /api/auth/login`, form-encoded `username` and `password`.
 *
 * @param {string} email
 * @param {string} password
 * @param {{token: string, headerName: string}} csrf the CSRF token to send
 */
function performLogin(email, password, csrf) {
    return $.ajax({
        method: "POST",
        url: "/api/auth/login",
        headers: {
            [csrf.headerName]: csrf.token
        },
        data: {
            username: email,
            password: password
        }
    });
}
