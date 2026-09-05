const toastEventName = "app:toast";

const allowedSeverities = new Set(["success", "info", "warning", "danger"]);

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
