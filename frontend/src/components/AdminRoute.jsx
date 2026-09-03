import { useContext } from "react";
import { Navigate, Outlet } from "react-router";
import AuthContext from "./AuthContext";

export default function AdminRoute() {
    const { currentUser } = useContext(AuthContext);

    if (currentUser?.role !== "ADMIN") {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}
