import $ from "jquery"

export function requestCsrfToken() {
    return $.ajax({
        method: "GET",
        url: "/api/csrf"
    });
}