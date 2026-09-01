import $ from "jquery"

export function getComments(reportId) {
    return $.ajax({
        method: "GET",
        url: `/api/reports/${reportId}/comments/`,
    })
}