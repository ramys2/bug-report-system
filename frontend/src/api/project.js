import $ from "jquery";
import { getCsrfToken } from "./csrf";

export function getAllProjects() {
    return $.ajax({
        method: "GET",
        url: "/api/projects",
    });
}

export function updateProjectName(projectId, name) {
    return patchProject(projectId, "name", { name });
}

export function updateProjectDescription(projectId, description) {
    return patchProject(projectId, "description", { description });
}

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
