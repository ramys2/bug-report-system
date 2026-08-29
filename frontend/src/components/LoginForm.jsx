import { useState } from "react";
import { login } from "../api/auth";
import "./LoginForm.css";
import { useNavigate } from "react-router";

function LoginForm() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    function handleSubmit(event) {
        event.preventDefault();

        login(email, password)
            .done((data, textStatus, xhr) => {
                navigate("/");
            })
            .fail((xhr) => {
                console.log("Login failed");
                console.log("Status:", xhr.status);
                console.log("Response:", xhr.responseText);
            });
    }

    return (
        <form className="border rounded-4 p-4" onSubmit={handleSubmit}>
            <div className="mb-3">
                <label className="visually-hidden" htmlFor="email">
                    Email
                </label>
                <input
                    type="email"
                    id="email"
                    className="form-control"
                    placeholder="Email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                />
            </div>

            <div className="mb-4">
                <label className="visually-hidden" htmlFor="password">
                    Password
                </label>
                <input
                    type="password"
                    id="password"
                    className="form-control"
                    placeholder="Password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                />
            </div>

            <button type="submit" className="btn btn-primary w-100">
                Sign In
            </button>

            <div className="border-top my-3" />

            <p className="mb-2">Don't have an account?</p>
            <button type="button" className="btn btn-outline-primary w-100">
                Register
            </button>
        </form>
    );
}

export default LoginForm;
