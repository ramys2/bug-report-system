import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import "./Modal.css";

/**
 * Popup with Bootstrap styling, controlled by the parent's React state.
 *
 * Focus stays inside the open popup and returns to its opener on close.
 * Children stay mounted when hidden so closing a form preserves its draft.
 *
 * @param {object} props
 * @param {import("react").ReactNode} props.children content of the dialog, e.g. one of the create forms
 * @param {boolean} [props.fullscreen=true] show the dialog full screen
 * @param {boolean} props.show whether the modal is open
 * @param {string} props.label accessible name of the dialog
 * @param {() => void} props.onHide requests closing; the parent should set `show` to `false`
 */
function Modal({ children, fullscreen = true, show, label, onHide }) {
    const modalRef = useRef(null);
    const pointerStartedOutside = useRef(false);

    useEffect(() => {
        if (!show) {
            return;
        }

        const modal = modalRef.current;
        const opener = document.activeElement;
        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        modal.focus({ preventScroll: true });

        function keepFocusInside(event) {
            if (!modal.contains(event.target)) {
                modal.focus({ preventScroll: true });
            }
        }

        document.addEventListener("focusin", keepFocusInside);
        return () => {
            document.removeEventListener("focusin", keepFocusInside);
            document.body.style.overflow = previousOverflow;
            if (opener instanceof HTMLElement && opener.isConnected) {
                opener.focus({ preventScroll: true });
            }
        };
    }, [show]);

    function handleKeyDown(event) {
        if (event.key === "Escape") {
            event.preventDefault();
            event.stopPropagation();
            onHide();
            return;
        }

        if (event.key !== "Tab") {
            return;
        }

        // Query on each Tab press because forms can add fields or disable buttons.
        const modal = modalRef.current;
        const controls = [...modal.querySelectorAll(
            'a[href], button, input, select, textarea, [tabindex], [contenteditable="true"]'
        )].filter((element) => element.tabIndex >= 0
            && !element.matches(":disabled") && element.getClientRects().length > 0);
        const first = controls[0];
        const last = controls[controls.length - 1];

        if (!first) {
            event.preventDefault();
            modal.focus();
        } else if (event.shiftKey && (document.activeElement === first || document.activeElement === modal)) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === modal)) {
            event.preventDefault();
            first.focus();
        }
    }

    // Render outside page containers so their layout cannot clip the popup.
    return createPortal(
        <div
            aria-label={label}
            aria-modal={show ? "true" : undefined}
            className="modal react-modal"
            hidden={!show}
            onKeyDown={handleKeyDown}
            onPointerDown={(event) => {
                pointerStartedOutside.current = event.target === event.currentTarget;
            }}
            onClick={(event) => {
                // Dragging from the content onto the backdrop should not close the dialog.
                if (pointerStartedOutside.current && event.target === event.currentTarget) {
                    onHide();
                }
                pointerStartedOutside.current = false;
            }}
            ref={modalRef}
            role="dialog"
            tabIndex={-1}
        >
            <div className={`modal-dialog modal-dialog-scrollable${fullscreen ? " modal-fullscreen" : ""}`}>
                <div className="modal-content">
                    {children}
                </div>
            </div>
        </div>,
        document.body
    );
}

export default Modal;
