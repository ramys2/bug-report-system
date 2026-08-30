import $ from "jquery"
import { getCsrfToken } from "./csrf";

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

export function createReport(report) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: "/api/reports",
        contentType: "application/json",
        data: JSON.stringify(report),
        headers: {
            [csrfToken.headerName]: csrfToken.token
        }
    });
}
