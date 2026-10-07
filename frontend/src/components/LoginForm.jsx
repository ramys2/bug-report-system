import { useContext, useState } from "react";
import { login } from "../api/auth";
import "./LoginForm.css";
import { useLocation, useNavigate } from "react-router";
import AuthContext from "./AuthContext";
import { showToast } from "./toast";

/**
 * Email and password form.
 *
 * On submit it calls `login()` (`POST /api/auth/login`); on success it reloads the current user through `AuthContext` and navigates back to the
 * page the user originally requested (`location.state.from`) or to `/`. A failed login shows an error toast: wrong credentials (HTTP 401) get a specific message, any other failure a generic one.
 * The Register button is a placeholder and does nothing yet. Takes no props.
 */
function LoginForm() {
    const auth = useContext(AuthContext);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();
    const location = useLocation();

    function handleSubmit(event) {
        event.preventDefault();

        login(email, password)
            .done(() => {
                auth.loadCurrentUser()
                    .done(() => navigate(location.state?.from ?? "/", { replace: true }));
            })
            .fail((xhr) => {
                const message = xhr.status === 401
                    ? "Invalid email or password."
                    : "Unable to sign in. Please try again later.";
                showToast("danger", message, "Login failed");
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
