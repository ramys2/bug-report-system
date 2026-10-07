import { useEffect, useRef } from "react";
import { Modal as BootstrapModal } from "bootstrap";

/**
 * Bootstrap modal shell (`.modal` > `.modal-dialog` > `.modal-content`).
 *
 * Two ways to open and close it:
 * - controlled: pass `show` and `onHide`. React state decides whether the modal is open; Bootstrap's `bootstrap.Modal` is only used for the animation and focus handling.
 * - uncontrolled (legacy, still used by pages that were not migrated yet): pass `id` and open/close it with `data-bs-*` attributes or `bootstrap.Modal` looked up by that id.
 *
 * @param {object} props
 * @param {import("react").ReactNode} props.children content of the dialog, e.g. one of the create forms
 * @param {boolean} [props.fullscreen=true] show the dialog full screen
 * @param {string} [props.id] element id used to find the modal in uncontrolled mode
 * @param {boolean} [props.show] controlled mode: whether the modal is open
 * @param {() => void} [props.onHide] controlled mode: called after the modal was hidden, including by Esc, a backdrop click or a `data-bs-dismiss` button. The parent should set `show` to `false`.
 */
function Modal({ children, fullscreen = true, id, show, onHide }) {
    const modalRef = useRef(null);

    useEffect(() => {
        if (show === undefined) {
            return;
        }

        const modal = BootstrapModal.getOrCreateInstance(modalRef.current);
        if (show) {
            modal.show();
        } else {
            modal.hide();
        }
    }, [show]);

    useEffect(() => {
        const modalElement = modalRef.current;
        if (!onHide) {
            return undefined;
        }

        modalElement.addEventListener("hidden.bs.modal", onHide);
        return () => modalElement.removeEventListener("hidden.bs.modal", onHide);
    }, [onHide]);

    return (
        <div id={id} className="modal" ref={modalRef} tabIndex={-1}>
            <div className={`modal-dialog modal-dialog-scrollable${fullscreen ? " modal-fullscreen" : ""}`}>
                <div className="modal-content">
                    {children}
                </div>
            </div>
        </div>
    );
}

export default Modal;
