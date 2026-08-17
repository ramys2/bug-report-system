import $ from "jquery";

export function login(email, password) {
    return $.ajax({
        method: "POST",
        url: "/login",
        data: {
            email: email,
            password: password
        },
    });
}