import { useContext } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import AuthContext from "./AuthContext";

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
