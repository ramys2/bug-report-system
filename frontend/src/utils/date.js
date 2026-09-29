/**
 * Formats a timestamp as `dd-MM-yyyy HH:mm` in the browser's local time zone.
 *
 * @param {string|null|undefined} value an ISO-8601 timestamp, or a string already in `dd-MM-yyyy HH:mm` form (returned unchanged)
 * @returns {string} the formatted text; `""` for an empty value, and the input itself if it cannot be parsed
 */
export function formatDateTime(value) {
    if (!value) {
        return "";
    }

    if (/^\d{2}-\d{2}-\d{4} \d{2}:\d{2}$/.test(value)) {
        return value;
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    const pad = (number) => String(number).padStart(2, "0");

    return `${pad(date.getDate())}-${pad(date.getMonth() + 1)}-${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
