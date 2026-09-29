/**
 * Bootstrap modal shell (`.modal` > `.modal-dialog` > `.modal-content`). It is opened and closed by Bootstrap's own JavaScript
 * (`data-bs-*` attributes / `bootstrap.Modal`), not by React state.
 *
 * @param {object} props
 * @param {import("react").ReactNode} props.children content of the dialog, e.g. one of the create forms
 * @param {boolean} [props.fullscreen=true] show the dialog full screen
 * @param {string} props.id element id used to find the modal, e.g. to open it
 */
function Modal({ children, fullscreen = true, id }) {
    return (
        <div id={id} className="modal" tabIndex={-1}>
            <div className={`modal-dialog modal-dialog-scrollable${fullscreen ? " modal-fullscreen" : ""}`}>
                <div className="modal-content">
                    {children}
                </div>
            </div>
        </div>
    );
}

export default Modal;
