import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import Modal from "../components/Modal";
import CreateComponentForm from "../components/CreateComponentForm";
import EditableName from "../components/EditableName";
import EditDescriptionModal from "../components/EditDescriptionModal";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "../components/toast";
import {
    getAllComponents,
    updateComponentDescription,
    updateComponentName,
    updateComponentResponsibleUser,
} from "../api/component";
import { searchUsers } from "../api/account";
import "./ComponentAdminPage.css";

/**
 * Table cell that shows the responsible user and lets the user pick another one by searching users by name (`GET /api/accounts/users?search=...`).
 * Clicking a search result saves it immediately with `PATCH /api/components/{id}/responsibleUserId`. Results of outdated searches are ignored.
 *
 * @param {object} props
 * @param {{id: string, name: string, responsibleUserName: string|null}} props.component the component shown
 * @param {(componentId: string, userName: string) => void} props.onResponsibleUserSaved called with the new user's name after it was saved
 */
function EditableResponsibleUser({ component, onResponsibleUserSaved }) {
    const { t, i18n } = useTranslation();
    const [isEditing, setIsEditing] = useState(false);
    const [search, setSearch] = useState("");
    const [users, setUsers] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        const normalizedSearch = search.trim();

        if (!isEditing || !normalizedSearch) {
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
    }, [isEditing, search, i18n]);

    function startEditing() {
        setSearch("");
        setUsers([]);
        setIsSearching(false);
        setIsEditing(true);
    }

    function changeSearch(value) {
        setSearch(value);

        if (value.trim()) {
            setIsSearching(true);
        } else {
            setUsers([]);
            setIsSearching(false);
        }
    }

    function saveResponsibleUser(user) {
        if (isSaving) {
            return;
        }

        setIsSaving(true);
        updateComponentResponsibleUser(component.id, user.id)
            .done(() => {
                onResponsibleUserSaved(component.id, user.name);
                setIsEditing(false);
                setSearch("");
                setUsers([]);
            })
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.updateFailed")))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <button
                aria-label={t("admin.component.editResponsibleUser", { name: component.name })}
                className="btn btn-outline-secondary btn-sm"
                onClick={startEditing}
                type="button"
            >
                <span>{component.responsibleUserName || t("reports.unassigned")}</span>
                <i aria-hidden="true" className="bi bi-pencil ms-2" />
            </button>
        );
    }

    return (
        <div className="position-relative">
            <div className="d-flex align-items-center gap-2">
                <input
                    aria-label={t("admin.component.searchResponsibleUser", { name: component.name })}
                    autoFocus
                    className="form-control form-control-sm"
                    disabled={isSaving}
                    onChange={(event) => changeSearch(event.target.value)}
                    placeholder={component.responsibleUserName || t("admin.user.searchUsers")}
                    value={search}
                />
                <button
                    className="btn btn-outline-secondary btn-sm"
                    disabled={isSaving}
                    onClick={() => setIsEditing(false)}
                    type="button"
                >
                    {t("common.cancel")}
                </button>
            </div>
            {search.trim() && (
                <div className="responsible-user-options border rounded bg-white mt-1">
                    {isSearching ? (
                        <p className="text-secondary small m-2">{t("common.searching")}</p>
                    ) : users.length === 0 ? (
                        <p className="text-secondary small m-2">{t("admin.user.noMatchingUsers")}</p>
                    ) : users.map((user) => (
                        <button
                            className="btn btn-light d-block text-start w-100 rounded-0"
                            disabled={isSaving}
                            key={user.id}
                            onClick={() => saveResponsibleUser(user)}
                            type="button"
                        >
                            {isSaving ? t("common.saving") : user.name}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}

/**
 * Admin page at `/admin/components` (ADMIN or DEVELOPER): all components in a table with inline name and responsible-user editing and a modal for the description.
 *
 * Components are loaded with `GET /api/components`; changes use the `PATCH /api/components/{id}/...` endpoints and "Create new" opens `CreateComponentForm`. Takes no props.
 */
export default function ComponentAdminPage() {
    const { t, i18n } = useTranslation();
    const [components, setComponents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [componentForDescription, setComponentForDescription] = useState(null);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    const loadComponents = useCallback(() => {
        return getAllComponents()
            .done(setComponents)
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("errors.loadComponentsTitle")))
            .always(() => setIsLoading(false));
    }, [i18n]);

    useEffect(() => {
        loadComponents();
    }, [loadComponents]);

    function updateComponent(componentId, changes) {
        setComponents((currentComponents) => currentComponents.map((component) => (
            component.id === componentId ? { ...component, ...changes } : component
        )));
    }

    function handleComponentCreated() {
        loadComponents();
        setIsCreateOpen(false);
    }

    function handleDescriptionSaved(componentId, description) {
        updateComponent(componentId, { description });
        setComponentForDescription(null);
    }

    return (
        <div className="component-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">{t("admin.component.heading")}</h1>
                        <p className="text-secondary mb-0">{t("admin.component.description")}</p>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        <span className="text-secondary small">{t("admin.component.count", { count: components.length })}</span>
                        <button
                            className="btn btn-primary"
                            onClick={() => setIsCreateOpen(true)}
                            type="button"
                        >
                            {t("reports.createNew")}
                        </button>
                    </div>
                </div>

                <section className="border rounded-4 d-flex flex-column flex-grow-1 overflow-hidden">
                    <div className="table-responsive flex-grow-1 overflow-auto">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light sticky-top">
                                <tr>
                                    <th scope="col">{t("common.id")}</th>
                                    <th scope="col">{t("common.name")}</th>
                                    <th scope="col">{t("reports.description")}</th>
                                    <th scope="col">{t("admin.component.responsibleUser")}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="4">{t("admin.component.loading")}</td></tr>
                                ) : components.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="4">{t("admin.component.empty")}</td></tr>
                                ) : components.map((component) => (
                                    <tr key={component.id}>
                                        <td className="small text-break">{component.id}</td>
                                        <td>
                                            <EditableName
                                                id={component.id}
                                                name={component.name}
                                                onNameSaved={(id, name) => updateComponent(id, { name })}
                                                onSave={updateComponentName}
                                            />
                                        </td>
                                        <td>
                                            <div className="component-description mb-2">{component.description || t("common.noDescription")}</div>
                                            <button className="btn btn-outline-secondary btn-sm" onClick={() => setComponentForDescription(component)} type="button">
                                                <i aria-hidden="true" className="bi bi-pencil me-1" />{t("common.editDescription")}
                                            </button>
                                        </td>
                                        <td>
                                            <EditableResponsibleUser
                                                component={component}
                                                onResponsibleUserSaved={(id, responsibleUserName) => updateComponent(id, { responsibleUserName })}
                                            />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </main>

            <EditDescriptionModal
                item={componentForDescription}
                label="component"
                onHide={() => setComponentForDescription(null)}
                onSave={updateComponentDescription}
                onSaved={handleDescriptionSaved}
            />
            <Modal label={t("admin.component.createHeading")} onHide={() => setIsCreateOpen(false)} show={isCreateOpen}>
                <CreateComponentForm onCreated={handleComponentCreated} onHide={() => setIsCreateOpen(false)} />
            </Modal>
        </div>
    );
}
