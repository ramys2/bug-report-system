import { Link, useNavigate } from "react-router";
import "./Navbar.css";
import { useContext } from "react";
import { useTranslation } from "react-i18next";
import AuthContext from "./AuthContext";
import LanguageSwitcher from "./LanguageSwitcher";

/**
 * Top navigation bar. Shows the brand and language switcher while nobody is signed in; otherwise links to Home, and by role also to
 * Users (ADMIN) and Components and Projects (ADMIN or DEVELOPER), plus the user's name and a log-out button.
 * Logging out calls `auth.logout()` and then navigates to `/login`. Takes no props; reads `AuthContext`.
 */
function Navbar() {
    const auth = useContext(AuthContext);
    const navigate = useNavigate();
    const { t } = useTranslation();

    function handleLogout() {
        auth.logout()
            .done(() => {
                void navigate("/login", { replace: true });
            });
    }

    if (auth.currentUser === undefined || auth.currentUser === null) {
        return (
            <nav className="navbar border-bottom px-4 py-3 gap-3">
                <span className="navbar-brand mb-0 fs-4">{t("navbar.brand")}</span>
                <LanguageSwitcher />
            </nav>
        );
    }

    return (
        <nav className="navbar border-bottom px-4 py-3">
            <div className="container-fluid d-flex flex-wrap justify-content-between gap-3 px-0">
                <div className="d-flex flex-wrap align-items-center gap-3 gap-md-4">
                    <span className="navbar-brand mb-0 fs-4">{t("navbar.brand")}</span>
                    <Link to="/" className="link-secondary link-underline-opacity-100">
                        {t("navbar.home")}
                    </Link>
                    {auth.currentUser.role === "ADMIN" && (
                        <>
                            <Link to="/admin/users" className="link-secondary link-underline-opacity-100">
                                {t("navbar.users")}
                            </Link>
                        </>
                    )}
                    {(auth.currentUser.role === "ADMIN" || auth.currentUser.role === "DEVELOPER") && (
                        <>
                            <Link to="/admin/components" className="link-secondary link-underline-opacity-100">
                                {t("navbar.components")}
                            </Link>
                            <Link to="/admin/projects" className="link-secondary link-underline-opacity-100">
                                {t("navbar.projects")}
                            </Link>
                        </>
                    )}
                </div>

                <div className="d-flex flex-wrap align-items-center gap-3">
                    <span
                        className="d-flex align-items-center gap-2 text-break link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-person-circle fs-4" />
                        {auth.currentUser.name}
                    </span>
                    <LanguageSwitcher />
                    <button
                        type="button"
                        onClick={handleLogout}
                        className="btn btn-link p-0 link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-box-arrow-right me-1" />
                        {t("navbar.logout")}
                    </button>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
