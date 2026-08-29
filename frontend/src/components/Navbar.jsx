import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router";
import { getCurrentUser, logout } from "../api/auth";
import "./Navbar.css";

function Navbar() {
    const [user, setUser] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        getCurrentUser()
            .done((currentUser) => setUser(currentUser));
    }, []);

    function handleLogout() {
        logout()
            .done(() => {
                setUser(null);
                navigate("/login", { replace: true });
            });
    }

    if (user === null) {
        return null;
    }

    return (
        <nav className="navbar border-bottom px-4 py-3">
            <div className="container-fluid d-flex justify-content-between px-0">
                <div className="d-flex align-items-center gap-4">
                    <span className="navbar-brand mb-0 fs-4">Bug Report</span>
                    <Link to="/" className="link-secondary link-underline-opacity-100">
                        Home
                    </Link>
                </div>

                <div className="d-flex align-items-center gap-3">
                    <span
                        className="d-flex align-items-center gap-2 link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-person-circle fs-4" />
                        {user.username}
                    </span>
                    <button
                        type="button"
                        onClick={handleLogout}
                        className="btn btn-link p-0 link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-box-arrow-right me-1" />
                        Log out
                    </button>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
