import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router";
import { getCurrentUser } from "../api/auth";

function ProtectedRoute() {
    const [isAuthenticated, setIsAuthenticated] = useState(null);
    const location = useLocation();

    useEffect(() => {
        getCurrentUser()
            .done(() => setIsAuthenticated(true))
            .fail(() => setIsAuthenticated(false));
    }, []);

    if (isAuthenticated === null) {
        return null;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    return <Outlet />;
}

export default ProtectedRoute;
