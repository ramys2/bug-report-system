import $ from "jquery";
import { requestCsrfToken } from "./csrf";

export function login(email, password) {
    return requestCsrfToken()
        .then((csrf) => {
            return performLogin(email, password, csrf);
        });
}

export function getCurrentUser() {
    return $.ajax({
        method: "GET",
        url: "/api/auth/me"
    });
}

export function logout() {
    return requestCsrfToken()
        .then((csrf) => {
            return $.ajax({
                method: "POST",
                url: "/api/auth/logout",
                headers: {
                    [csrf.headerName]: csrf.token
                }
            });
        });
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
