import { useEffect, useRef, useState } from "react";
import "./Toast.css";
import { toastEventName } from "./toast";

function Toast() {
    const [toasts, setToasts] = useState([]);
    const timeoutIds = useRef(new Map());

    function removeToast(id) {
        const timeoutId = timeoutIds.current.get(id);
        clearTimeout(timeoutId);
        timeoutIds.current.delete(id);
        setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== id));
    }

    useEffect(() => {
        const timeouts = timeoutIds.current;

        function addToast(event) {
            const id = crypto.randomUUID();
            const toast = { id, ...event.detail };

            setToasts((currentToasts) => [...currentToasts, toast]);
            timeouts.set(id, setTimeout(() => removeToast(id), 5000));
        }

        window.addEventListener(toastEventName, addToast);

        return () => {
            window.removeEventListener(toastEventName, addToast);
            timeouts.forEach((timeoutId) => clearTimeout(timeoutId));
            timeouts.clear();
        };
    }, []);

    return (
        <div className="toast-stack" aria-live="polite" aria-atomic="true">
            {toasts.map((toast) => (
                <div key={toast.id} className={`app-toast app-toast-${toast.severity}`} role="status">
                    <div>
                        {toast.summary && <strong className="app-toast-summary">{toast.summary}</strong>}
                        {toast.message && <p className="app-toast-message">{toast.message}</p>}
                    </div>
                    <button
                        type="button"
                        className="btn-close"
                        aria-label="Close notification"
                        onClick={() => removeToast(toast.id)}
                    />
                </div>
            ))}
        </div>
    );
}

export default Toast;
