import { useContext } from "react";
import { Navigate, Outlet } from "react-router";
import AuthContext from "./AuthContext";

export default function DeveloperRoute() {
    const { currentUser } = useContext(AuthContext);

    if (currentUser?.role !== "ADMIN" && currentUser?.role !== "DEVELOPER") {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}
