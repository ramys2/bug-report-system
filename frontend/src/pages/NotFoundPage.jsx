import { Link } from "react-router";

/**
 * Page shown for any path that no other route matches (the `*` route in `App.jsx`). Offers a link back to `/`. Takes no props.
 */
function NotFoundPage() {
    return (
        <main className="container d-flex flex-column flex-grow-1 align-items-center justify-content-center py-5 text-center">
            <h1 className="display-4">Page not found</h1>
            <p className="text-muted">The page you are looking for does not exist.</p>
            <Link to="/" className="btn btn-primary">
                Back to home
            </Link>
        </main>
    );
}

export default NotFoundPage;
