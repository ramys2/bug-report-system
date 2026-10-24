import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { createComponent } from "../api/component";
import { searchUsers } from "../api/account";
import { getAllProjects } from "../api/project";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "./toast";

/**
 * Empty form state for name, project and description; also used to reset the form after a successful submit.
 */
const initialFormValues = { name: "", projectId: "", description: "" };

/**
 * Form (meant to sit inside a `Modal`) for creating a component with `POST /api/components`.
 *
 * The projects to choose from are loaded once when the form is mounted (`GET /api/projects`). The responsible user is picked by searching users by name
 * (`GET /api/accounts/users?search=...`); results of outdated searches are ignored.
 * Name and project are required; the responsible user is optional. Failures show localized messages resolved from backend error codes or a safe fallback.
 * On success the form is cleared and `onCreated` is called.
 *
 * @param {object} props
 * @param {() => void} props.onHide requests closing the modal
 * @param {() => void} props.onCreated called after the component was created
 */
export default function CreateComponentForm({ onCreated, onHide }) {
    const { t, i18n } = useTranslation();
    const [formValues, setFormValues] = useState(initialFormValues);
    const [projects, setProjects] = useState([]);
    const [responsibleUser, setResponsibleUser] = useState(null);
    const [search, setSearch] = useState("");
    const [users, setUsers] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        getAllProjects()
            .done(setProjects)
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.loadProjectsTitle")));
    }, [i18n]);

    useEffect(() => {
        const normalizedSearch = search.trim();

        if (!normalizedSearch) {
            return undefined;
        }

        let isCurrentSearch = true;

        searchUsers(normalizedSearch)
            .done((matchingUsers) => {
                if (isCurrentSearch) {
                    setUsers(matchingUsers);
                }
            })
            .fail((xhr) => {
                if (isCurrentSearch) {
                    setUsers([]);
                    showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.searchFailed"));
                }
            })
            .always(() => {
                if (isCurrentSearch) {
                    setIsSearching(false);
                }
            });

        return () => {
            isCurrentSearch = false;
        };
    }, [search, i18n]);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({ ...currentValues, [name]: value }));
    }

    function changeSearch(value) {
        setSearch(value);
        setResponsibleUser(null);

        if (value.trim()) {
            setIsSearching(true);
        } else {
            setUsers([]);
            setIsSearching(false);
        }
    }

    function selectResponsibleUser(user) {
        setResponsibleUser(user);
        setSearch("");
        setUsers([]);
        setIsSearching(false);
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (!formValues.name.trim() || !formValues.projectId) {
            showToast("warning", t("admin.component.requiredFields"), t("common.missingInformation"));
            return;
        }

        setIsSubmitting(true);
        createComponent({
            ...formValues,
            name: formValues.name.trim(),
            responsibleUserId: responsibleUser ? responsibleUser.id : null,
        })
            .done(() => {
                setFormValues(initialFormValues);
                setResponsibleUser(null);
                onCreated();
            })
            .fail((xhr) => {
                showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.createComponentTitle"));
            })
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">{t("admin.component.createHeading")}</h1>
                <button aria-label={t("common.close")} className="btn-close" onClick={onHide} type="button" />
            </div>
            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-component-name">{t("common.name")} *</label>
                    <input
                        className="form-control"
                        id="new-component-name"
                        name="name"
                        onChange={handleChange}
                        required
                        value={formValues.name}
                    />
                </div>
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-component-project">{t("reports.project")} *</label>
                    <select
                        className="form-select"
                        id="new-component-project"
                        name="projectId"
                        onChange={handleChange}
                        required
                        value={formValues.projectId}
                    >
                        <option value="">{t("reports.selectProject")}</option>
                        {projects.map((project) => (
                            <option key={project.id} value={project.id}>
                                {project.name}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-component-description">{t("reports.description")}</label>
                    <textarea
                        className="form-control"
                        id="new-component-description"
                        name="description"
                        onChange={handleChange}
                        rows="4"
                        value={formValues.description}
                    />
                </div>
                <div>
                    <label className="form-label" htmlFor="new-component-responsible-user">{t("admin.component.responsibleUser")}</label>
                    {responsibleUser ? (
                        <div className="d-flex align-items-center gap-2">
                            <span>{responsibleUser.name}</span>
                            <button className="btn btn-outline-secondary btn-sm" onClick={() => setResponsibleUser(null)} type="button">{t("common.change")}</button>
                        </div>
                    ) : (
                        <>
                            <input
                                className="form-control"
                                disabled={isSubmitting}
                                id="new-component-responsible-user"
                                onChange={(event) => changeSearch(event.target.value)}
                                placeholder={t("admin.user.searchUsers")}
                                value={search}
                            />
                            {search.trim() && (
                                <div className="border rounded bg-white mt-1">
                                    {isSearching ? (
                                        <p className="text-secondary small m-2">{t("common.searching")}</p>
                                    ) : users.length === 0 ? (
                                        <p className="text-secondary small m-2">{t("admin.user.noMatchingUsers")}</p>
                                    ) : users.map((user) => (
                                        <button
                                            className="btn btn-light d-block text-start w-100 rounded-0"
                                            disabled={isSubmitting}
                                            key={user.id}
                                            onClick={() => selectResponsibleUser(user)}
                                            type="button"
                                        >
                                            {user.name}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
            <div className="modal-footer">
                <button className="btn btn-secondary" onClick={onHide} type="button">{t("common.cancel")}</button>
                <button className="btn btn-primary" disabled={isSubmitting} type="submit">
                    {isSubmitting ? t("common.creating") : t("admin.component.create")}
                </button>
            </div>
        </form>
    );
}
