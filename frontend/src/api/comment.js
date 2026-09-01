import $ from "jquery"
import { getCsrfToken } from "./csrf";

export function getComments(reportId) {
    return $.ajax({
        method: "GET",
        url: `/api/reports/${reportId}/comments`,
    })
}

export function createComment(reportId, comment) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: `/api/reports/${reportId}/comments`,
        contentType: "application/json",
        data: JSON.stringify(comment),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
