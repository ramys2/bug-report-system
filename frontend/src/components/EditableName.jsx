import { useState } from "react";
import { useTranslation } from "react-i18next";
import { showToast } from "./toast";

/**
 * Table cell content that shows a name and lets the user rename it inline. Save is disabled for empty or unchanged names.
 *
 * @param {object} props
 * @param {string} props.id id of the renamed item, passed to `onSave`
 * @param {string} props.name current name
 * @param {"project"|"component"} props.label item type used to choose the error translation
 * @param {(id: string, name: string) => import("jquery").jqXHR} props.onSave sends the new name to the backend, e.g. `updateProjectName`
 * @param {(id: string, name: string) => void} props.onNameSaved called with the new name after it was saved
 */
export default function EditableName({ id, name, label, onSave, onNameSaved }) {
    const { t, i18n } = useTranslation();
    const [isEditing, setIsEditing] = useState(false);
    const [draftName, setDraftName] = useState(name);
    const [isSaving, setIsSaving] = useState(false);

    function startEditing() {
        setDraftName(name);
        setIsEditing(true);
    }

    function saveName() {
        if (isSaving) {
            return;
        }

        setIsSaving(true);
        onSave(id, draftName)
            .done(() => {
                onNameSaved(id, draftName);
                setIsEditing(false);
            })
            .fail(() => showToast("danger", i18n.t(`admin.${label}.updateNameError`), i18n.t("errors.updateFailed")))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <div className="d-flex align-items-center gap-2">
                <span>{name}</span>
                <button
                    aria-label={t("common.editNameFor", { name })}
                    className="btn btn-outline-secondary btn-sm"
                    onClick={startEditing}
                    type="button"
                >
                    <i aria-hidden="true" className="bi bi-pencil" />
                </button>
            </div>
        );
    }

    return (
        <div className="d-flex align-items-center gap-2">
            <input
                aria-label={t("common.nameFor", { name })}
                className="form-control form-control-sm"
                disabled={isSaving}
                onChange={(event) => setDraftName(event.target.value)}
                value={draftName}
            />
            <button
                className="btn btn-primary btn-sm"
                disabled={isSaving || !draftName.trim() || draftName === name}
                onClick={saveName}
                type="button"
            >
                {isSaving ? t("common.saving") : t("common.save")}
            </button>
            <button
                className="btn btn-outline-secondary btn-sm"
                disabled={isSaving}
                onClick={() => setIsEditing(false)}
                type="button"
            >
                {t("common.cancel")}
            </button>
        </div>
    );
}
