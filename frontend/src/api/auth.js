import $ from "jquery";
import { cleanCsrfToken, getCsrfToken, loadCsrfToken } from "./csrf";

export function login(email, password) {
    return loadCsrfToken()
        .then(() => performLogin(email, password, getCsrfToken()))
        .then(() => loadCsrfToken());
}

export function getCurrentUser() {
    return $.ajax({
        method: "GET",
        url: "/api/auth/me"
    });
}

export function logout() {
    return loadCsrfToken()
        .then(() => {
            const csrfToken = getCsrfToken();

            return $.ajax({
                method: "POST",
                url: "/api/auth/logout",
                headers: {
                    [csrfToken.headerName]: csrfToken.token
                }
            });
        })
        .then(() => cleanCsrfToken());
}

function performLogin(email, password, csrf) {
    return $.ajax({
        method: "POST",
        url: "/api/auth/login",
        headers: {
            [csrf.headerName]: csrf.token
        },
        data: {
            username: email,
            password: password
        }
    });
}
