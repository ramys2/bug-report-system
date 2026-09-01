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

export function getReport(reportId) {
    return $.ajax({
        method: "GET",
        url:`/api/reports/${reportId}`
    })
}

export function updateAssignee(reportId, assigneeId) {
    return patchReport(reportId, "assignee", { assigneeId });
}

export function updateSeverity(reportId, severity) {
    return patchReport(reportId, "severity", { severity });
}

export function updateStatus(reportId, status) {
    return patchReport(reportId, "status", { status });
}

export function updateProject(reportId, projectId) {
    return patchReport(reportId, "project", { projectId });
}

export function updateComponent(reportId, componentId) {
    return patchReport(reportId, "component", { componentId });
}

export function updateDescription(reportId, description) {
    return patchReport(reportId, "description", { description });
}

export function updateStepsToReproduce(reportId, stepsToReproduce) {
    return patchReport(reportId, "steps-to-reproduce", { stepsToReproduce });
}

export function updateExpectedBehavior(reportId, expectedBehavior) {
    return patchReport(reportId, "expected-behavior", { expectedBehavior });
}

export function updateActualBehavior(reportId, actualBehavior) {
    return patchReport(reportId, "actual-behavior", { actualBehavior });
}

function patchReport(reportId, field, request) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/reports/${reportId}/${field}`,
        contentType: "application/json",
        data: JSON.stringify(request),
        headers: {
            [csrfToken.headerName]: csrfToken.token
        }
    });
}
