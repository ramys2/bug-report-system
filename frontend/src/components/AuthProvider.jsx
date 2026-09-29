import { useCallback, useEffect, useState } from "react";
import { getCurrentUser, logout as logoutRequest } from "../api/auth";
import { loadCsrfToken } from "../api/csrf";
import AuthContext from "./AuthContext";

/**
 * Provides `AuthContext` to the whole app and keeps the signed-in user in state.
 *
 * On mount it calls `GET /api/auth/me` (via `getCurrentUser`); on success it stores the user and loads the CSRF token, on failure
 * it sets the user to `null`. `loadCurrentUser()` repeats this check (used after login) and `logout()` calls `POST /api/auth/logout`.
 *
 * @param {object} props
 * @param {import("react").ReactNode} props.children the part of the app that can read the context
 */
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
