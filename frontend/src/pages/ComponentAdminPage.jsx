import { useEffect, useState } from "react";
import { Modal as BootstrapModal } from "bootstrap";
import Modal from "../components/Modal";
import {
    getAllComponents,
    updateComponentDescription,
    updateComponentName,
    updateComponentResponsibleUser,
} from "../api/component";
import { searchUsers } from "../api/account";
import "./ComponentAdminPage.css";

const descriptionModalId = "component-description-modal";

function EditableName({ component, onNameSaved }) {
    const [isEditing, setIsEditing] = useState(false);
    const [draftName, setDraftName] = useState(component.name);
    const [isSaving, setIsSaving] = useState(false);

    function startEditing() {
        setDraftName(component.name);
        setIsEditing(true);
    }

    function saveName() {
        if (isSaving) {
            return;
        }

        setIsSaving(true);
        updateComponentName(component.id, draftName)
            .done(() => {
                onNameSaved(component.id, draftName);
                setIsEditing(false);
            })
            .fail(() => alert("Unable to update the component name."))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <div className="d-flex align-items-center gap-2">
                <span>{component.name}</span>
                <button
                    aria-label={`Edit name for ${component.name}`}
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
                aria-label={`Name for ${component.name}`}
                className="form-control form-control-sm"
                disabled={isSaving}
                onChange={(event) => setDraftName(event.target.value)}
                value={draftName}
            />
            <button
                className="btn btn-primary btn-sm"
                disabled={isSaving || !draftName.trim() || draftName === component.name}
                onClick={saveName}
                type="button"
            >
                {isSaving ? "Saving..." : "Save"}
            </button>
            <button
                className="btn btn-outline-secondary btn-sm"
                disabled={isSaving}
                onClick={() => setIsEditing(false)}
                type="button"
            >
                Cancel
            </button>
        </div>
    );
}

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
                    alert("Unable to search for users.");
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
        updateComponentResponsibleUser(component.id, user.userId)
            .done(() => {
                onResponsibleUserSaved(component.id, user.name);
                setIsEditing(false);
                setSearch("");
                setUsers([]);
            })
            .fail(() => alert("Unable to update the responsible user."))
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
                            key={user.userId}
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

export default function ComponentAdminPage() {
    const [components, setComponents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [componentForDescription, setComponentForDescription] = useState(null);
    const [draftDescription, setDraftDescription] = useState("");
    const [isSavingDescription, setIsSavingDescription] = useState(false);

    useEffect(() => {
        getAllComponents()
            .done(setComponents)
            .fail(() => alert("Unable to fetch components."))
            .always(() => setIsLoading(false));
    }, []);

    function updateComponent(componentId, changes) {
        setComponents((currentComponents) => currentComponents.map((component) => (
            component.id === componentId ? { ...component, ...changes } : component
        )));
    }

    function openDescriptionEditor(component) {
        setComponentForDescription(component);
        setDraftDescription(component.description || "");

        const modalElement = document.getElementById(descriptionModalId);
        BootstrapModal.getOrCreateInstance(modalElement).show();
    }

    function closeDescriptionEditor() {
        const modalElement = document.getElementById(descriptionModalId);
        BootstrapModal.getOrCreateInstance(modalElement).hide();
    }

    function saveDescription() {
        if (!componentForDescription || isSavingDescription) {
            return;
        }

        setIsSavingDescription(true);
        updateComponentDescription(componentForDescription.id, draftDescription)
            .done(() => {
                updateComponent(componentForDescription.id, { description: draftDescription });
                closeDescriptionEditor();
            })
            .fail(() => alert("Unable to update the component description."))
            .always(() => setIsSavingDescription(false));
    }

    return (
        <div className="component-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">Component administration</h1>
                        <p className="text-secondary mb-0">Manage component details and responsible users.</p>
                    </div>
                    <span className="text-secondary small">{components.length} component{components.length === 1 ? "" : "s"}</span>
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
                                        <td><EditableName component={component} onNameSaved={(id, name) => updateComponent(id, { name })} /></td>
                                        <td>
                                            <div className="component-description mb-2">{component.description || "No description"}</div>
                                            <button className="btn btn-outline-secondary btn-sm" onClick={() => openDescriptionEditor(component)} type="button">
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

            <Modal id={descriptionModalId}>
                <div className="modal-header">
                    <h2 className="modal-title fs-5">Edit component description</h2>
                </div>
                <div className="modal-body d-flex flex-column">
                    <label className="form-label" htmlFor="component-description">Description</label>
                    <textarea
                        className="form-control flex-grow-1"
                        disabled={isSavingDescription}
                        id="component-description"
                        onChange={(event) => setDraftDescription(event.target.value)}
                        value={draftDescription}
                    />
                </div>
                <div className="modal-footer">
                    <button className="btn btn-outline-secondary" disabled={isSavingDescription} onClick={closeDescriptionEditor} type="button">Cancel</button>
                    <button className="btn btn-primary" disabled={isSavingDescription} onClick={saveDescription} type="button">
                        {isSavingDescription ? "Saving..." : "Save"}
                    </button>
                </div>
            </Modal>
        </div>
    );
}
