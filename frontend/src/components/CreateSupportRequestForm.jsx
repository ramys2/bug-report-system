import { useState } from "react";
import { useTranslation } from "react-i18next";
import { createSupportRequest } from "../api/support";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "./toast";

const initialFormValues = { type: "ROLE_CHANGE", requestedRole: "", description: "" };

/**
 * Support form inside the Navbar modal. The existing hierarchy allows only Reporter to Developer.
 * A failed request preserves the draft; success clears it and asks the parent to close the modal.
 *
 * @param {object} props
 * @param {"REPORTER"|"DEVELOPER"} props.currentRole the signed-in user's role
 * @param {() => void} props.onCreated called after successful creation
 * @param {() => void} props.onHide requests closing the modal
 */
export default function CreateSupportRequestForm({ currentRole, onCreated, onHide }) {
    const { t, i18n } = useTranslation();
    const [formValues, setFormValues] = useState(initialFormValues);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const eligibleRoles = currentRole === "REPORTER" ? ["DEVELOPER"] : [];
    const isRoleChange = formValues.type === "ROLE_CHANGE";
    const canSubmit = isRoleChange
        ? eligibleRoles.includes(formValues.requestedRole)
        : formValues.description.trim().length > 0;

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({ ...currentValues, [name]: value }));
    }

    function handleSubmit(event) {
        event.preventDefault();
        if (isSubmitting) {
            return;
        }
        if (!canSubmit) {
            showToast("warning", t(isRoleChange ? "support.roleRequired" : "support.descriptionRequired"),
                t("common.missingInformation"));
            return;
        }

        const request = isRoleChange
            ? { type: "ROLE_CHANGE", requestedRole: formValues.requestedRole }
            : { type: "OTHER", description: formValues.description.trim() };

        setIsSubmitting(true);
        createSupportRequest(request)
            .done(() => {
                setFormValues(initialFormValues);
                showToast("success", i18n.t("support.created"));
                onCreated();
            })
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("support.createFailed")))
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">{t("support.heading")}</h1>
                <button aria-label={t("common.close")} className="btn-close" onClick={onHide} type="button" />
            </div>
            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="support-request-type">{t("support.requestType")} *</label>
                    <select
                        className="form-select"
                        disabled={isSubmitting}
                        id="support-request-type"
                        name="type"
                        onChange={handleChange}
                        required
                        value={formValues.type}
                    >
                        <option value="ROLE_CHANGE">{t("support.roleChange")}</option>
                        <option value="OTHER">{t("support.other")}</option>
                    </select>
                </div>
                {isRoleChange ? (
                    <div>
                        <label className="form-label" htmlFor="support-request-role">{t("support.requestedRole")} *</label>
                        <select
                            aria-describedby={eligibleRoles.length === 0 ? "support-no-eligible-role" : undefined}
                            className="form-select"
                            disabled={isSubmitting || eligibleRoles.length === 0}
                            id="support-request-role"
                            name="requestedRole"
                            onChange={handleChange}
                            required
                            value={eligibleRoles.includes(formValues.requestedRole) ? formValues.requestedRole : ""}
                        >
                            <option value="">{t("support.selectRole")}</option>
                            {eligibleRoles.map((role) => (
                                <option key={role} value={role}>{t(`admin.roles.${role}`)}</option>
                            ))}
                        </select>
                        {eligibleRoles.length === 0 && (
                            <p className="form-text mb-0" id="support-no-eligible-role">{t("support.noEligibleRoles")}</p>
                        )}
                    </div>
                ) : (
                    <div>
                        <label className="form-label" htmlFor="support-request-description">{t("reports.description")} *</label>
                        <textarea
                            className="form-control"
                            disabled={isSubmitting}
                            id="support-request-description"
                            maxLength={10000}
                            name="description"
                            onChange={handleChange}
                            required
                            rows="4"
                            value={formValues.description}
                        />
                    </div>
                )}
            </div>
            <div className="modal-footer">
                <button className="btn btn-secondary" onClick={onHide} type="button">{t("common.cancel")}</button>
                <button className="btn btn-primary" disabled={isSubmitting || !canSubmit} type="submit">
                    {isSubmitting ? t("support.submitting") : t("support.submit")}
                </button>
            </div>
        </form>
    );
}
