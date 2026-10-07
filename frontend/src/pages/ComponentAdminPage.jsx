import { useEffect, useState } from "react";
import Modal from "../components/Modal";
import CreateComponentForm from "../components/CreateComponentForm";
import EditableName from "../components/EditableName";
import EditDescriptionModal from "../components/EditDescriptionModal";
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
            .fail(() => {
                if (isCurrentSearch) {
                    setUsers([]);
                    showToast("danger", "Unable to search for users.", "Search failed");
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
    }, [isEditing, search]);

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
            .fail(() => showToast("danger", "Unable to update the responsible user.", "Update failed"))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <button
                aria-label={`Edit responsible user for ${component.name}`}
                className="btn btn-outline-secondary btn-sm"
                onClick={startEditing}
                type="button"
            >
                <span>{component.responsibleUserName || "Unassigned"}</span>
                <i aria-hidden="true" className="bi bi-pencil ms-2" />
            </button>
        );
    }

    return (
        <div className="position-relative">
            <div className="d-flex align-items-center gap-2">
                <input
                    aria-label={`Search responsible user for ${component.name}`}
                    autoFocus
                    className="form-control form-control-sm"
                    disabled={isSaving}
                    onChange={(event) => changeSearch(event.target.value)}
                    placeholder={component.responsibleUserName || "Search users"}
                    value={search}
                />
                <button
                    className="btn btn-outline-secondary btn-sm"
                    disabled={isSaving}
                    onClick={() => setIsEditing(false)}
                    type="button"
                >
                    Cancel
                </button>
            </div>
            {search.trim() && (
                <div className="responsible-user-options border rounded bg-white mt-1">
                    {isSearching ? (
                        <p className="text-secondary small m-2">Searching...</p>
                    ) : users.length === 0 ? (
                        <p className="text-secondary small m-2">No matching users.</p>
                    ) : users.map((user) => (
                        <button
                            className="btn btn-light d-block text-start w-100 rounded-0"
                            disabled={isSaving}
                            key={user.id}
                            onClick={() => saveResponsibleUser(user)}
                            type="button"
                        >
                            {isSaving ? "Saving..." : user.name}
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
    const [components, setComponents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [componentForDescription, setComponentForDescription] = useState(null);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    useEffect(() => {
        loadComponents();
    }, []);

    function loadComponents() {
        return getAllComponents()
            .done(setComponents)
            .fail(() => showToast("danger", "Unable to fetch components.", "Unable to load components"))
            .always(() => setIsLoading(false));
    }

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
                        <h1 className="h3 mb-1">Component administration</h1>
                        <p className="text-secondary mb-0">Manage component details and responsible users.</p>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        <span className="text-secondary small">{components.length} component{components.length === 1 ? "" : "s"}</span>
                        <button
                            className="btn btn-primary"
                            onClick={() => setIsCreateOpen(true)}
                            type="button"
                        >
                            Create new +
                        </button>
                    </div>
                </div>

                <section className="border rounded-4 d-flex flex-column flex-grow-1 overflow-hidden">
                    <div className="table-responsive flex-grow-1 overflow-auto">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light sticky-top">
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">Name</th>
                                    <th scope="col">Description</th>
                                    <th scope="col">Responsible user</th>
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="4">Loading components...</td></tr>
                                ) : components.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="4">No components found.</td></tr>
                                ) : components.map((component) => (
                                    <tr key={component.id}>
                                        <td className="small text-break">{component.id}</td>
                                        <td>
                                            <EditableName
                                                id={component.id}
                                                label="component"
                                                name={component.name}
                                                onNameSaved={(id, name) => updateComponent(id, { name })}
                                                onSave={updateComponentName}
                                            />
                                        </td>
                                        <td>
                                            <div className="component-description mb-2">{component.description || "No description"}</div>
                                            <button className="btn btn-outline-secondary btn-sm" onClick={() => setComponentForDescription(component)} type="button">
                                                <i aria-hidden="true" className="bi bi-pencil me-1" />Edit description
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
            <Modal onHide={() => setIsCreateOpen(false)} show={isCreateOpen}>
                <CreateComponentForm onCreated={handleComponentCreated} />
            </Modal>
        </div>
    );
}
