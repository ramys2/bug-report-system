import $ from "jquery";
import { requestCsrfToken } from "./csrf";

export function login(email, password) {
    return requestCsrfToken()
        .then((csrf) => {
            return performLogin(email, password, csrf);
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
