import { BrowserRouter, Routes, Route } from "react-router";

import ProtectedRoute from "./components/ProtectedRoute";
import AdminRoute from "./components/AdminRoute";
import DeveloperRoute from "./components/DeveloperRoute";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import BugReportPage from "./pages/BugReportPage";
import UserAdminPage from "./pages/UserAdminPage";
import ComponentAdminPage from "./pages/ComponentAdminPage";
import ProjectAdminPage from "./pages/ProjectAdminPage";
import Navbar from "./components/Navbar";
import AuthProvider from "./components/AuthProvider";
import Toast from "./components/Toast";

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

                    <Route path="/login" element={<LoginPage />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
