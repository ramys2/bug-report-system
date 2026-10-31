import i18n from "../i18n";

// Keep this list aligned with the backend ErrorCode enum and both translation files.
const backendErrorCodes = new Set([
    "BUG_REPORT_NOT_FOUND",
    "PROJECT_NOT_FOUND",
    "COMPONENT_NOT_FOUND",
    "USER_NOT_FOUND",
    "COMMENT_NOT_FOUND",
    "DUPLICATE_EMAIL",
    "DUPLICATE_USERNAME",
    "BUG_REPORT_ALREADY_CLOSED",
    "BUG_REPORT_CLOSED",
    "CLOSED_REPORT_COMMENTS_FORBIDDEN",
    "RESOLUTION_REQUIRED",
    "ASSIGNEE_REQUIRED",
    "INVALID_STATUS_TRANSITION",
    "COMPONENT_PROJECT_MISMATCH",
    "ASSIGNEE_NOT_DEVELOPER",
    "LAST_ADMIN_REQUIRED",
    "DEVELOPER_HAS_OPEN_ASSIGNMENTS",
    "SUPPORT_ROLE_NOT_ALLOWED",
    "SUPPORT_REQUEST_ALREADY_PENDING",
    "SUPPORT_REQUEST_NOT_FOUND",
    "SUPPORT_REQUEST_ALREADY_RESOLVED",
    "SUPPORT_INVALID_RESOLUTION_STATUS",
    "SUPPORT_ROLE_CHANGE_OUTDATED",
    "INVALID_REQUEST",
    "DATA_CONFLICT",
    "AUTHENTICATION_REQUIRED",
    "INVALID_CREDENTIALS",
    "GOOGLE_EMAIL_CONFLICT",
    "GOOGLE_EMAIL_NOT_VERIFIED",
    "GOOGLE_LOGIN_FAILED",
    "ACCESS_DENIED",
    "INTERNAL_ERROR"
]);

/**
 * Resolves a failed jQuery request into text in the currently selected language.
 * Only known codes are accepted; backend messages and response text are never displayed.
 *
 * @param {{status?: number, responseJSON?: {code?: string}}|null|undefined} xhr failed request
 * @returns {string} localized error message
 */
export function getApiErrorMessage(xhr) {
    if (xhr?.status === 0) {
        return i18n.t("errors.NETWORK_ERROR");
    }

    const code = xhr?.responseJSON?.code;
    if (backendErrorCodes.has(code)) {
        return i18n.t(`errors.${code}`);
    }

    // Proxies can return a gateway error or HTML while the backend is unavailable.
    if ([502, 503, 504].includes(xhr?.status)) {
        return i18n.t("errors.BACKEND_UNAVAILABLE");
    }

    return i18n.t("errors.UNKNOWN_ERROR");
}
