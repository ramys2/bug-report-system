import { useContext } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import AuthContext from "./AuthContext";

/**
 * Route guard for pages that need a signed-in user; renders the nested routes via `<Outlet />`.
 *
 * While the session is still being checked it renders nothing; if nobody is signed in it redirects to `/login` and remembers
 * the requested location in the router state, so `LoginForm` can send the user back afterwards.
 */
function ProtectedRoute() {
    const auth = useContext(AuthContext);
    const location = useLocation();

    if (auth.currentUser === undefined) {
        return null;
    }

    if (auth.currentUser === null) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    return <Outlet />;
}

export default ProtectedRoute;
