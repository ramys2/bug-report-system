import { useContext } from "react";
import { Navigate, Outlet } from "react-router";
import AuthContext from "./AuthContext";

/**
 * Route guard that only lets users with the ADMIN or DEVELOPER role through (nested routes via `<Outlet />`); everyone else is redirected to `/`.
 * This only hides the pages; the backend enforces the same rule.
 */
export default function DeveloperRoute() {
    const { currentUser } = useContext(AuthContext);

    if (currentUser?.role !== "ADMIN" && currentUser?.role !== "DEVELOPER") {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}
