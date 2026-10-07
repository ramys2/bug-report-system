import $ from "jquery";
import { getCsrfToken } from "./csrf";

/**
 * `GET /api/components`: lists all components, or only those of one project.
 *
 * @param {string} [projectId] if given, only the components of this project are returned
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, name, description, responsibleUserName, projectId }`; `projectId` is `null` for components created before they belonged to projects
 */
export function getAllComponents(projectId) {
    return $.ajax({
        method: "GET",
        url: "/api/components",
        data: { projectId },
    });
}

/**
 * `POST /api/components` (ADMIN or DEVELOPER).
 *
 * @param {{name: string, projectId: string, description?: string, responsibleUserId?: string}} component `name` must not be blank; `projectId` must belong to an existing project; `responsibleUserId` is optional, but must belong to an existing user
 * @returns {JQuery.jqXHR} resolves (201) with `{ id, message }`; fails with 404 and `{ message }` if the project or the user does not exist
 */
export function createComponent(component) {
    return postComponent(component);
}

/**
 * `PATCH /api/components/{componentId}/name` (ADMIN or DEVELOPER).
 *
 * @param {string} componentId
 * @param {string} name must not be blank
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateComponentName(componentId, name) {
    return patchComponent(componentId, "name", { name });
}

/**
 * `PATCH /api/components/{componentId}/description` (ADMIN or DEVELOPER).
 *
 * @param {string} componentId
 * @param {string} description
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateComponentDescription(componentId, description) {
    return patchComponent(componentId, "description", { description });
}

/**
 * `PATCH /api/components/{componentId}/responsibleUserId` (ADMIN or DEVELOPER).
 *
 * @param {string} componentId
 * @param {string} responsibleUserId id of the new responsible user
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateComponentResponsibleUser(componentId, responsibleUserId) {
    return patchComponent(componentId, "responsibleUserId", { responsibleUserId });
}

/**
 * Shared helper for the component `PATCH` calls. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} componentId
 * @param {string} field last path segment, e.g. `"name"`
 * @param {object} request JSON body
 * @returns {JQuery.jqXHR}
 */
function patchComponent(componentId, field, request) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/components/${componentId}/${field}`,
        contentType: "application/json",
        data: JSON.stringify(request),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}

/**
 * Sends `POST /api/components`, with the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {object} request JSON body
 * @returns {JQuery.jqXHR}
 */
function postComponent(request) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: "/api/components",
        contentType: "application/json",
        data: JSON.stringify(request),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
