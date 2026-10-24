import { useState } from "react";
import { useTranslation } from "react-i18next";
import { createProject } from "../api/project";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "./toast";

/**
 * Empty form state; also used to reset the form after a successful submit.
 */
const initialFormValues = { name: "", description: "" };

/**
 * Form (meant to sit inside a `Modal`) for creating a project with `POST /api/projects`. Only the name is required.
 * On success the form is cleared and `onCreated` is called; on failure an error toast is shown.
 *
 * @param {object} props
 * @param {() => void} props.onHide requests closing the modal
 * @param {() => void} props.onCreated called after the project was created
 */
export default function CreateProjectForm({ onCreated, onHide }) {
    const { t, i18n } = useTranslation();
    const [formValues, setFormValues] = useState(initialFormValues);
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({ ...currentValues, [name]: value }));
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (!formValues.name.trim()) {
            showToast("warning", t("admin.project.nameRequired"), t("common.missingInformation"));
            return;
        }

        setIsSubmitting(true);
        createProject({ ...formValues, name: formValues.name.trim() })
            .done(() => {
                setFormValues(initialFormValues);
                onCreated();
            })
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.createProjectTitle")))
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">{t("admin.project.createHeading")}</h1>
                <button aria-label={t("common.close")} className="btn-close" onClick={onHide} type="button" />
            </div>
            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-project-name">{t("common.name")} *</label>
                    <input
                        className="form-control"
                        id="new-project-name"
                        name="name"
                        onChange={handleChange}
                        required
                        value={formValues.name}
                    />
                </div>
                <div>
                    <label className="form-label" htmlFor="new-project-description">{t("reports.description")}</label>
                    <textarea
                        className="form-control"
                        id="new-project-description"
                        name="description"
                        onChange={handleChange}
                        rows="4"
                        value={formValues.description}
                    />
                </div>
            </div>
            <div className="modal-footer">
                <button className="btn btn-secondary" onClick={onHide} type="button">{t("common.cancel")}</button>
                <button className="btn btn-primary" disabled={isSubmitting} type="submit">
                    {isSubmitting ? t("common.creating") : t("admin.project.create")}
                </button>
            </div>
        </form>
    );
}
