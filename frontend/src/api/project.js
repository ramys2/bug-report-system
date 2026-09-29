import $ from "jquery";
import { getCsrfToken } from "./csrf";

/**
 * `GET /api/projects`: lists all projects.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, name, description }`
 */
export function getAllProjects() {
    return $.ajax({
        method: "GET",
        url: "/api/projects",
    });
}

/**
 * `POST /api/projects` (ADMIN only).
 *
 * @param {{name: string, description?: string}} project `name` must not be blank
 * @returns {JQuery.jqXHR} resolves (201) with `{ id, message }`
 */
export function createProject(project) {
    return postProject(project);
}

/**
 * `PATCH /api/projects/{projectId}/name` (ADMIN or DEVELOPER).
 *
 * @param {string} projectId
 * @param {string} name must not be blank
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateProjectName(projectId, name) {
    return patchProject(projectId, "name", { name });
}

/**
 * `PATCH /api/projects/{projectId}/description` (ADMIN or DEVELOPER).
 *
 * @param {string} projectId
 * @param {string} description
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateProjectDescription(projectId, description) {
    return patchProject(projectId, "description", { description });
}

/**
 * Shared helper for the project `PATCH` calls. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} projectId
 * @param {string} field last path segment, e.g. `"name"`
 * @param {object} request JSON body
 * @returns {JQuery.jqXHR}
 */
function patchProject(projectId, field, request) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/projects/${projectId}/${field}`,
        contentType: "application/json",
        data: JSON.stringify(request),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}

/**
 * Sends `POST /api/projects`, with the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {object} request JSON body
 * @returns {JQuery.jqXHR}
 */
function postProject(request) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: "/api/projects",
        contentType: "application/json",
        data: JSON.stringify(request),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
