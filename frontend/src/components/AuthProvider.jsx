import { useCallback, useEffect, useState } from "react";
import { getCurrentUser, logout as logoutRequest } from "../api/auth";
import { loadCsrfToken } from "../api/csrf";
import AuthContext from "./AuthContext";

export default function AuthProvider({ children }) {
    const [currentUser, setCurrentUser] = useState(undefined);

    const loadCurrentUser = useCallback(() => {
        return getCurrentUser()
            .done((user) => {
                setCurrentUser(user);
                loadCsrfToken();
            })
            .fail(() => setCurrentUser(null));
    }, []);

    function logout() {
        return logoutRequest()
            .done(() => setCurrentUser(null));
    }

    useEffect(() => {
        loadCurrentUser();
    }, [loadCurrentUser]);

    return (
        <AuthContext.Provider value={{ currentUser, loadCurrentUser, logout }}>
            {children}
        </AuthContext.Provider>
    );
}
