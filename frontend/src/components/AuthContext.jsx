import { createContext } from "react";

/**
 * React context holding the authentication state, provided by `AuthProvider`.
 *
 * The value is `{ currentUser, loadCurrentUser, logout }`. `currentUser` is `undefined` while the session is being checked,
 * `null` when nobody is signed in, and otherwise the object returned by `GET /api/auth/me`
 * (`{ id, username, email, role }`, where `username` is the display name). `null` is the default when no provider is present.
 */
const AuthContext = createContext(null);

export default AuthContext;