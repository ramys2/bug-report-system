import { BrowserRouter, Routes, Route } from "react-router";

import ProtectedRoute from "./components/ProtectedRoute";
import AdminRoute from "./components/AdminRoute";
import DeveloperRoute from "./components/DeveloperRoute";
import GuestRoute from "./components/GuestRoute";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import NotFoundPage from "./pages/NotFoundPage";
import BugReportPage from "./pages/BugReportPage";
import UserAdminPage from "./pages/UserAdminPage";
import ComponentAdminPage from "./pages/ComponentAdminPage";
import ProjectAdminPage from "./pages/ProjectAdminPage";
import Navbar from "./components/Navbar";
import AuthProvider from "./components/AuthProvider";
import Toast from "./components/Toast";

/**
 * Root component: sets up routing and the app-wide providers.
 *
 * `AuthProvider` wraps everything, `Navbar` and `Toast` are always visible. Routes:
 * - `/login`: only for users who are not signed in (`GuestRoute`), `LoginPage`.
 * - `/` and `/reports/:id`: any signed-in user (`ProtectedRoute`).
 * - `/admin/components` and `/admin/projects`: ADMIN or DEVELOPER (`DeveloperRoute`).
 * - `/admin/users`: ADMIN only (`AdminRoute`).
 * - Any other path: `NotFoundPage`.
 *
 * The route guards only control what the UI shows; the backend enforces access itself.
 */
export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Navbar />
                <Toast />

                <Routes>
                    <Route element={<ProtectedRoute />}>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/reports/:id" element={<BugReportPage />} />
                        <Route element={<DeveloperRoute />}>
                            <Route path="/admin/components" element={<ComponentAdminPage />} />
                            <Route path="/admin/projects" element={<ProjectAdminPage />} />
                        </Route>
                        <Route element={<AdminRoute />}>
                            <Route path="/admin/users" element={<UserAdminPage />} />
                        </Route>
                    </Route>

                    <Route element={<GuestRoute />}>
                        <Route path="/login" element={<LoginPage />} />
                    </Route>

                    <Route path="*" element={<NotFoundPage />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
