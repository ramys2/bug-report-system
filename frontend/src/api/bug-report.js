import $ from "jquery"

export function getAllReports() {
    return $.ajax({
        method: "GET",
        url: "/api/reports",
    })
}