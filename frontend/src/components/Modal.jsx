import { useEffect, useRef } from "react";
import { Modal as BootstrapModal } from "bootstrap";

/**
 * Bootstrap modal shell (`.modal` > `.modal-dialog` > `.modal-content`), controlled by React state.
 *
 * `bootstrap.Modal` is only used for the animation and focus handling: an effect shows or hides it when `show` changes,
 * and Bootstrap's `hidden.bs.modal` event is forwarded to `onHide`. That event also fires for Esc, a backdrop click and `data-bs-dismiss` buttons,
 * so the parent's state stays in sync no matter how the modal was closed.
 *
 * @param {object} props
 * @param {import("react").ReactNode} props.children content of the dialog, e.g. one of the create forms
 * @param {boolean} [props.fullscreen=true] show the dialog full screen
 * @param {boolean} props.show whether the modal is open
 * @param {() => void} props.onHide called after the modal was hidden; the parent should set `show` to `false`
 */
function Modal({ children, fullscreen = true, show, onHide }) {
    const modalRef = useRef(null);

    useEffect(() => {
        const modal = BootstrapModal.getOrCreateInstance(modalRef.current);
        if (show) {
            modal.show();
        } else {
            modal.hide();
        }
    }, [show]);

    useEffect(() => {
        const modalElement = modalRef.current;
        modalElement.addEventListener("hidden.bs.modal", onHide);
        return () => modalElement.removeEventListener("hidden.bs.modal", onHide);
    }, [onHide]);

    return (
        <div className="modal" ref={modalRef} tabIndex={-1}>
            <div className={`modal-dialog modal-dialog-scrollable${fullscreen ? " modal-fullscreen" : ""}`}>
                <div className="modal-content">
                    {children}
                </div>
            </div>
        </div>
    );
}

export default Modal;
