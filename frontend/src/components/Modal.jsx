function Modal({ children, id }) {
    return (
        <div id={id} className="modal" tabIndex={-1}>
            <div className="modal-dialog modal-fullscreen">
                <div className="modal-content">
                    {children}
                </div>
            </div>
        </div>
    );
}

export default Modal;