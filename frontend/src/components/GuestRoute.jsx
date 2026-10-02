import { useContext } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import AuthContext from "./AuthContext";

/**
 * Route guard for pages that only make sense when nobody is signed in (the login page); renders the nested routes via `<Outlet />`.
 *
 * While the session is still being checked it renders nothing; if a user is signed in it redirects to the page remembered in the router
 * state by `ProtectedRoute` (`location.state.from`) or to `/`. This is the same target `LoginForm` navigates to after a successful login,
 * so both redirects agree even though this guard reacts first when the signed-in user is stored.
 */
export default function GuestRoute() {
    const { currentUser } = useContext(AuthContext);
    const location = useLocation();

    if (currentUser === undefined) {
        return null;
    }

    if (currentUser !== null) {
        return <Navigate to={location.state?.from ?? "/"} replace />;
    }

    return <Outlet />;
}
