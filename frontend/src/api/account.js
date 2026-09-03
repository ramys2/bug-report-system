import $ from "jquery";
import { getCsrfToken } from "./csrf";

export function getAllAccounts() {
    return $.ajax({
        method: "GET",
        url: "/api/accounts",
    });
}

export function updateAccountRole(userId, role) {
    const csrfToken = getCsrfToken();

    return $.ajax({
        method: "PATCH",
        url: `/api/accounts/${userId}/role`,
        contentType: "application/json",
        data: JSON.stringify({ role }),
        headers: {
            [csrfToken.headerName]: csrfToken.token,
        },
    });
}
