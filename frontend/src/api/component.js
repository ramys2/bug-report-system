import $ from "jquery";
import { getCsrfToken } from "./csrf";

export function getAllComponents() {
    return $.ajax({
        method: "GET",
        url: "/api/components",
    });
}

export function updateComponentName(componentId, name) {
    return patchComponent(componentId, "name", { name });
}

export function updateComponentDescription(componentId, description) {
    return patchComponent(componentId, "description", { description });
}

export function updateComponentResponsibleUser(componentId, responsibleUserId) {
    return patchComponent(componentId, "responsibleUserId", { responsibleUserId });
}

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
