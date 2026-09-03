import { BrowserRouter, Routes, Route } from "react-router";

import ProtectedRoute from "./components/ProtectedRoute";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import BugReportPage from "./pages/BugReportPage";
import Navbar from "./components/Navbar";
import AuthProvider from "./components/AuthProvider";

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Navbar />

                <Routes>
                    <Route element={<ProtectedRoute />}>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/reports/:id" element={<BugReportPage />} />
                    </Route>

                    <Route path="/login" element={<LoginPage />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
