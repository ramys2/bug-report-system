import $ from "jquery"
import { getCsrfToken } from "./csrf";

/**
 * @typedef {"LOW"|"MEDIUM"|"HIGH"|"CRITICAL"} Severity
 * @typedef {"OPEN"|"ASSIGNED"|"IN_PROGRESS"|"NEEDS_INFORMATION"|"REVIEWING"|"REJECTED"|"CLOSED"} Status
 */

/**
 * A report as listed by `GET /api/reports`, `/reported` and `/assigned`.
 * @typedef {object} BugReportBrief
 * @property {string} reportId
 * @property {string} title
 * @property {string} author display name of the reporter
 * @property {string|null} assignee display name of the assignee, `null` if unassigned
 * @property {Status} status
 * @property {Severity} severity
 * @property {string} createdAt already formatted as `dd-MM-yyyy HH:mm`
 */

/**
 * A report as returned by `GET /api/reports/{reportId}`.
 * @typedef {object} BugReport
 * @property {string} id
 * @property {string} reporterName
 * @property {string|null} assigneeName
 * @property {string} projectName
 * @property {string} componentName
 * @property {string} title
 * @property {string|null} description
 * @property {string|null} stepsToReproduce
 * @property {string|null} expectedBehavior
 * @property {string|null} actualBehavior
 * @property {Severity} severity
 * @property {Status} status
 * @property {string} createdAt ISO-8601 timestamp
 * @property {string} updatedAt ISO-8601 timestamp
 * @property {{id: string, description: string, resolvedAt: string, fixedVersion: string|null, commitUrl: string|null}|null} resolution `null` until the report is closed
 */

/**
 * Body of `POST /api/reports`.
 * @typedef {object} NewBugReport
 * @property {string} projectId
 * @property {string} componentId
 * @property {string} title
 * @property {Severity} severity
 * @property {string} [assigneeId] id of a developer
 * @property {string} [description]
 * @property {string} [stepsToReproduce]
 * @property {string} [expectedBehavior]
 * @property {string} [actualBehavior]
 */

/**
 * `GET /api/reports`: lists all reports.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `BugReportBrief`
 */
export function getAllReports() {
    return $.ajax({
        method: "GET",
        url: "/api/reports",
    })
}

/**
 * `GET /api/reports/reported`: lists the reports filed by the signed-in user.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `BugReportBrief`
 */
export function getReported() {
    return $.ajax({
        method: "GET",
        "url": "/api/reports/reported"
    })
}

/**
 * `GET /api/reports/assigned`: lists the reports assigned to the signed-in user.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `BugReportBrief`
 */
export function getAssigned() {
    return $.ajax({
        method: "GET",
        "url": "/api/reports/assigned"
    })
}

/**
 * `POST /api/reports`: files a new report. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {NewBugReport} report the report data
 * @returns {JQuery.jqXHR} resolves (201) with `{ id, message }`
 */
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

/**
 * `POST /api/reports/{reportId}/resolution`: closes a report. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} reportId id of the report
 * @param {{description: string, fixedVersion?: string, commitUrl?: string}} resolution how the bug was resolved; `description` is required
 * @returns {JQuery.jqXHR} resolves (201) with `{ reportId, message }`; rejects with 409 if already closed
 */
export function closeReport(reportId, resolution) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "POST",
        url: `/api/reports/${reportId}/resolution`,
        contentType: "application/json",
        data: JSON.stringify(resolution),
        headers: {
            [csrfToken.headerName]: csrfToken.token
        }
    });
}

/**
 * `GET /api/reports/{reportId}`: fetches the full detail of one report.
 *
 * @param {string} reportId id of the report
 * @returns {JQuery.jqXHR} resolves with a `BugReport`; rejects with 404 if it does not exist
 */
export function getReport(reportId) {
    return $.ajax({
        method: "GET",
        url:`/api/reports/${reportId}`
    })
}

/**
 * `PATCH /api/reports/{reportId}/assignee`: assigns the report to a developer.
 *
 * @param {string} reportId
 * @param {string} assigneeId id of a user with the DEVELOPER role
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`; 409 if the user is not a developer or the report is closed
 */
export function updateAssignee(reportId, assigneeId) {
    return patchReport(reportId, "assignee", { assigneeId });
}

/**
 * `PATCH /api/reports/{reportId}/severity`.
 *
 * @param {string} reportId
 * @param {Severity} severity
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateSeverity(reportId, severity) {
    return patchReport(reportId, "severity", { severity });
}

/**
 * `PATCH /api/reports/{reportId}/status`. `CLOSED` is rejected (409); use `closeReport()` instead.
 *
 * @param {string} reportId
 * @param {Status} status
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateStatus(reportId, status) {
    return patchReport(reportId, "status", { status });
}

/**
 * `PATCH /api/reports/{reportId}/project`.
 *
 * @param {string} reportId
 * @param {string} projectId
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateProject(reportId, projectId) {
    return patchReport(reportId, "project", { projectId });
}

/**
 * `PATCH /api/reports/{reportId}/component`.
 *
 * @param {string} reportId
 * @param {string} componentId
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateComponent(reportId, componentId) {
    return patchReport(reportId, "component", { componentId });
}

/**
 * `PATCH /api/reports/{reportId}/description`.
 *
 * @param {string} reportId
 * @param {string} description
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateDescription(reportId, description) {
    return patchReport(reportId, "description", { description });
}

/**
 * `PATCH /api/reports/{reportId}/steps-to-reproduce`.
 *
 * @param {string} reportId
 * @param {string} stepsToReproduce
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateStepsToReproduce(reportId, stepsToReproduce) {
    return patchReport(reportId, "steps-to-reproduce", { stepsToReproduce });
}

/**
 * `PATCH /api/reports/{reportId}/expected-behavior`.
 *
 * @param {string} reportId
 * @param {string} expectedBehavior
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateExpectedBehavior(reportId, expectedBehavior) {
    return patchReport(reportId, "expected-behavior", { expectedBehavior });
}

/**
 * `PATCH /api/reports/{reportId}/actual-behavior`.
 *
 * @param {string} reportId
 * @param {string} actualBehavior
 * @returns {JQuery.jqXHR} resolves with `{ id, message }`
 */
export function updateActualBehavior(reportId, actualBehavior) {
    return patchReport(reportId, "actual-behavior", { actualBehavior });
}

/**
 * Shared helper for the report `PATCH` calls above. Sends the cached CSRF token (see csrf.js), which `login()` and `AuthProvider` load once the user is signed in.
 *
 * @param {string} reportId
 * @param {string} field last path segment, e.g. `"severity"`
 * @param {object} request JSON body
 * @returns {JQuery.jqXHR}
 */
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
