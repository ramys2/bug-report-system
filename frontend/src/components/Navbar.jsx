import { Link, useNavigate } from "react-router";
import "./Navbar.css";
import { useContext } from "react";
import AuthContext from "./AuthContext";

/**
 * Top navigation bar. Shows only the brand while nobody is signed in; otherwise links to Home, and by role also to
 * Users (ADMIN) and Components and Projects (ADMIN or DEVELOPER), plus the user's name and a log-out button.
 * Logging out calls `auth.logout()` and then navigates to `/login`. Takes no props; reads `AuthContext`.
 */
function Navbar() {
    const auth = useContext(AuthContext);
    const navigate = useNavigate();

    function handleLogout() {
        auth.logout()
            .done(() => {
                navigate("/login", { replace: true });
            });
    }

    if (auth.currentUser === undefined || auth.currentUser === null) {
        return (
            <nav className="navbar border-bottom px-4 py-3">
                <span className="navbar-brand mb-0 fs-4">Bug Report</span>
            </nav>
        );
    }

    return (
        <nav className="navbar border-bottom px-4 py-3">
            <div className="container-fluid d-flex justify-content-between px-0">
                <div className="d-flex align-items-center gap-4">
                    <span className="navbar-brand mb-0 fs-4">Bug Report</span>
                    <Link to="/" className="link-secondary link-underline-opacity-100">
                        Home
                    </Link>
                    {auth.currentUser.role === "ADMIN" && (
                        <>
                            <Link to="/admin/users" className="link-secondary link-underline-opacity-100">
                                Users
                            </Link>
                        </>
                    )}
                    {(auth.currentUser.role === "ADMIN" || auth.currentUser.role === "DEVELOPER") && (
                        <>
                            <Link to="/admin/components" className="link-secondary link-underline-opacity-100">
                                Components
                            </Link>
                            <Link to="/admin/projects" className="link-secondary link-underline-opacity-100">
                                Projects
                            </Link>
                        </>
                    )}
                </div>

                <div className="d-flex align-items-center gap-3">
                    <span
                        className="d-flex align-items-center gap-2 link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-person-circle fs-4" />
                        {auth.currentUser.username}
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
