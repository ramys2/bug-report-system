import $ from "jquery"

let csrfToken = null;
let csrfHeaderName = null;

export function loadCsrfToken() {
    return $.ajax({
        method: "GET",
        url: "/api/csrf"
    }).then((data) => {
        csrfToken = data.token;
        csrfHeaderName = data.headerName;
    });
}

export function getCsrfToken() {
    return {
        token: csrfToken,
        headerName: csrfHeaderName
    }
}

export function cleanCsrfToken() {
    csrfToken = null;
    csrfHeaderName = null;
}
