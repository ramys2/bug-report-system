import { BrowserRouter, Routes, Route } from "react-router";

import ProtectedRoute from "./components/ProtectedRoute";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route element={<ProtectedRoute />}>
                    <Route path="/" element={<HomePage />} />
                </Route>
                <Route path="/login" element={<LoginPage />} />
            </Routes>
        </BrowserRouter>
    );
}
