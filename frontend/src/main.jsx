import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./index.css";
import App from "./App.jsx";

/**
 * Application entry point: loads the global styles (Bootstrap, Bootstrap Icons, index.css) and renders `App` into `#root` in React strict mode.
 */
createRoot(document.getElementById("root")).render(
    <StrictMode>
        <App />
    </StrictMode>
);
