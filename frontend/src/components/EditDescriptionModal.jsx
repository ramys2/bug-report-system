import { useState } from "react";
import Modal from "./Modal";
import { showToast } from "./toast";

/**
 * Modal for editing the description of one item (a project or a component). It is open while `item` is not `null`.
 *
 * The parent keeps the selected item in state: set it to open the modal, and set it back to `null` in `onHide` and `onSaved`.
 * The draft text is reset to the item's description each time a (new) item is selected.
 *
 * @param {object} props
 * @param {{id: string, description: string|null}|null} props.item item being edited, or `null` when the modal is closed
 * @param {string} props.label what is being edited, e.g. "project"; used in the title, element id and error toast
 * @param {(id: string, description: string) => import("jquery").jqXHR} props.onSave sends the new description to the backend, e.g. `updateProjectDescription`
 * @param {(id: string, description: string) => void} props.onSaved called with the new description after it was saved
 * @param {() => void} props.onHide called when the modal was closed without saving
 */
export default function EditDescriptionModal({ item, label, onSave, onSaved, onHide }) {
    const [draftDescription, setDraftDescription] = useState("");
    const [shownItem, setShownItem] = useState(null);
    const [isSaving, setIsSaving] = useState(false);

    // Reset the draft when a new item is selected (adjusting state during render is React's recommended alternative to an effect here).
    // Nothing is reset when `item` goes back to `null`, so the text does not blank out while the modal fades away.
    if (item !== shownItem) {
        setShownItem(item);
        if (item) {
            setDraftDescription(item.description || "");
        }
    }

    function saveDescription() {
        if (!item || isSaving) {
            return;
        }

        setIsSaving(true);
        onSave(item.id, draftDescription)
            .done(() => onSaved(item.id, draftDescription))
            .fail(() => showToast("danger", `Unable to update the ${label} description.`, "Update failed"))
            .always(() => setIsSaving(false));
    }

    return (
        <Modal onHide={onHide} show={item !== null}>
            <div className="modal-header">
                <h2 className="modal-title fs-5">Edit {label} description</h2>
                <button aria-label="Close" className="btn-close" disabled={isSaving} onClick={onHide} type="button" />
            </div>
            <div className="modal-body d-flex flex-column">
                <label className="form-label" htmlFor={`${label}-description`}>Description</label>
                <textarea
                    className="form-control flex-grow-1"
                    disabled={isSaving}
                    id={`${label}-description`}
                    onChange={(event) => setDraftDescription(event.target.value)}
                    value={draftDescription}
                />
            </div>
            <div className="modal-footer">
                <button className="btn btn-outline-secondary" disabled={isSaving} onClick={onHide} type="button">Cancel</button>
                <button className="btn btn-primary" disabled={isSaving} onClick={saveDescription} type="button">
                    {isSaving ? "Saving..." : "Save"}
                </button>
            </div>
        </Modal>
    );
}
