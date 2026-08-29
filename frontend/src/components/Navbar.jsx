import "./Navbar.css";

function Navbar() {
    return (
        <nav className="navbar border-bottom px-4 py-3">
            <div className="container-fluid d-flex justify-content-between px-0">
                <div className="d-flex align-items-center gap-4">
                    <span className="navbar-brand mb-0 fs-4">Bug Report</span>
                    <a href="#home" className="link-secondary link-underline-opacity-100">
                        Home
                    </a>
                </div>

                <div className="d-flex align-items-center gap-3">
                    <a
                        href="#profile"
                        className="d-flex align-items-center gap-2 link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-person-circle fs-4" />
                        user1
                    </a>
                    <a
                        href="#logout"
                        className="link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
                    >
                        <i aria-hidden="true" className="bi bi-box-arrow-right me-1" />
                        Log out
                    </a>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
