import { useState } from "react";
import { login } from "../api/auth";

function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    function handleSubmit(event) {
        event.preventDefault();

    login(email, password)
        .done((data, textStatus, xhr) => {
            console.log("Login successful:", xhr.status);
        })
        .fail((xhr) => {
            console.log("Login failed");
            console.log("Status:", xhr.status);
            console.log("Response:", xhr.responseText);
        });
    }

    return (
        <div className="container mt-5">
            <h1>Login</h1>

            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="email" className="form-label">
                        Email
                    </label>

                    <input
                        type="email"
                        id="email"
                        className="form-control"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                    />
                </div>

                <div className="mb-3">
                    <label htmlFor="password" className="form-label">
                        Password
                    </label>

                    <input
                        type="password"
                        id="password"
                        className="form-control"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                    />
                </div>

                <button type="submit" className="btn btn-primary">
                    Login
                </button>
            </form>
        </div>
    );
}

export default LoginPage;