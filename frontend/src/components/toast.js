/**
 * Name of the browser event that carries toast requests from `showToast()` to the `Toast` component.
 */
const toastEventName = "app:toast";

/**
 * Bootstrap contextual colors accepted as toast severity.
 */
const allowedSeverities = new Set(["success", "info", "warning", "danger"]);

/**
 * Accepts either positional arguments or one options object and returns a valid toast: `"error"` becomes `"danger"`,
 * unknown severities become `"info"`, and message/summary are turned into strings.
 *
 * @param {string|{severity?: string, message?: string, summary?: string}} severity
 * @param {string} [message]
 * @param {string} [summary]
 * @returns {{severity: string, message: string, summary: string}}
 */
function normalizeToast(severity, message, summary) {
    const options = typeof severity === "object" && severity !== null
        ? severity
        : { severity, message, summary };

    const normalizedSeverity = options.severity === "error" ? "danger" : options.severity;

    return {
        severity: allowedSeverities.has(normalizedSeverity) ? normalizedSeverity : "info",
        message: String(options.message ?? ""),
        summary: String(options.summary ?? ""),
    };
}

/**
 * Shows a notification in the application's top-right toast stack.
 *
 * Both forms are supported:
 * showToast("danger", "Unable to save the report.", "Save failed");
 * showToast({ severity: "danger", message: "Unable to save the report.", summary: "Save failed" });
 */
export function showToast(severity = "info", message = "", summary = "") {
    if (typeof window === "undefined") {
        return;
    }

    window.dispatchEvent(new CustomEvent(toastEventName, {
        detail: normalizeToast(severity, message, summary),
    }));
}

export { toastEventName };
