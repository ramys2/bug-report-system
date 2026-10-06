import $ from "jquery"
import { getCsrfToken } from "./csrf";

/**
 * `GET /api/comments?reportId=...`: lists the comments of a report, newest first.
 *
 * @param {string} reportId
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, bugReportId, authorId, authorName, content, createdAt }`
 */
export function getComments(reportId) {
    return $.ajax({
        method: "GET",
        url: "/api/comments",
        data: { reportId },
    })
}

/**
 * `POST /api/comments`: adds a comment to a report. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} reportId
 * @param {{content: string}} comment the text; must not be blank
 * @returns {JQuery.jqXHR} resolves (201) with `{ id, authorId, authorName, content, createdAt }`; 409 if the report is closed
 */
export function createComment(reportId, comment) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: "/api/comments",
        contentType: "application/json",
        data: JSON.stringify({ reportId, ...comment }),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}

/**
 * `DELETE /api/comments/{commentId}`: deletes a comment (allowed for its author or an admin). Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} commentId
 * @returns {JQuery.jqXHR} resolves with no content (204); 403 for other users, 409 if the report is closed
 */
export function removeComment(commentId) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "DELETE",
        url: `/api/comments/${commentId}`,
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
