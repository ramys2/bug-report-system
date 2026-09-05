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
