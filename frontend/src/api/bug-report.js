import $ from "jquery"

export function getAllReports() {
    return $.ajax({
        method: "GET",
        url: "/api/reports",
    })
}

export function getReported() {
    return $.ajax({
        method: "GET",
        "url": "/api/reports/reported"
    })
}

export function getAssigned() {
    return $.ajax({
        method: "GET",
        "url": "/api/reports/assigned"
    })
}