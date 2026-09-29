import { useContext, useEffect, useState } from "react";
import { Modal as BootstrapModal } from "bootstrap";
import Modal from "../components/Modal";
import AuthContext from "../components/AuthContext";
import CreateProjectForm from "../components/CreateProjectForm";
import { showToast } from "../components/toast";
import { getAllProjects, updateProjectDescription, updateProjectName } from "../api/project";
import "./ProjectAdminPage.css";

/**
 * Element id of the modal for editing a project's description.
 */
const descriptionModalId = "project-description-modal";
/**
 * Element id of the modal for creating a project.
 */
const createModalId = "create-project-modal";

/**
 * Table cell that shows a project's name and lets the user rename it inline. Save calls `PATCH /api/projects/{id}/name` and is disabled for empty or unchanged names.
 *
 * @param {object} props
 * @param {{id: string, name: string}} props.project the project shown
 * @param {(projectId: string, name: string) => void} props.onNameSaved called with the new name after it was saved
 */
function EditableName({ project, onNameSaved }) {
    const [isEditing, setIsEditing] = useState(false);
    const [draftName, setDraftName] = useState(project.name);
    const [isSaving, setIsSaving] = useState(false);

    function startEditing() {
        setDraftName(project.name);
        setIsEditing(true);
    }

    function saveName() {
        if (isSaving) {
            return;
        }

        setIsSaving(true);
        updateProjectName(project.id, draftName)
            .done(() => {
                onNameSaved(project.id, draftName);
                setIsEditing(false);
            })
            .fail(() => showToast("danger", "Unable to update the project name.", "Update failed"))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <div className="d-flex align-items-center gap-2">
                <span>{project.name}</span>
                <button
                    aria-label={`Edit name for ${project.name}`}
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
                aria-label={`Name for ${project.name}`}
                className="form-control form-control-sm"
                disabled={isSaving}
                onChange={(event) => setDraftName(event.target.value)}
                value={draftName}
            />
            <button
                className="btn btn-primary btn-sm"
                disabled={isSaving || !draftName.trim() || draftName === project.name}
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

/**
 * Admin page at `/admin/projects` (ADMIN or DEVELOPER): all projects in a table with inline name editing and a modal for the description.
 *
 * Projects are loaded with `GET /api/projects`. Names and descriptions are saved with `PATCH /api/projects/{id}/name` and `/description`.
 * The "Create new" button (`POST /api/projects` through `CreateProjectForm`) is only shown to admins, matching the backend rule. Takes no props.
 */
export default function ProjectAdminPage() {
    const auth = useContext(AuthContext);
    const [projects, setProjects] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [projectForDescription, setProjectForDescription] = useState(null);
    const [draftDescription, setDraftDescription] = useState("");
    const [isSavingDescription, setIsSavingDescription] = useState(false);

    useEffect(() => {
        loadProjects();
    }, []);

    function loadProjects() {
        return getAllProjects()
            .done(setProjects)
            .fail(() => showToast("danger", "Unable to fetch projects.", "Unable to load projects"))
            .always(() => setIsLoading(false));
    }

    function updateProject(projectId, changes) {
        setProjects((currentProjects) => currentProjects.map((project) => (
            project.id === projectId ? { ...project, ...changes } : project
        )));
    }

    function openDescriptionEditor(project) {
        setProjectForDescription(project);
        setDraftDescription(project.description || "");

        const modalElement = document.getElementById(descriptionModalId);
        BootstrapModal.getOrCreateInstance(modalElement).show();
    }

    function closeDescriptionEditor() {
        const modalElement = document.getElementById(descriptionModalId);
        BootstrapModal.getOrCreateInstance(modalElement).hide();
    }

    function handleProjectCreated() {
        loadProjects();

        const modalElement = document.getElementById(createModalId);
        BootstrapModal.getOrCreateInstance(modalElement).hide();
    }

    function saveDescription() {
        if (!projectForDescription || isSavingDescription) {
            return;
        }

        setIsSavingDescription(true);
        updateProjectDescription(projectForDescription.id, draftDescription)
            .done(() => {
                updateProject(projectForDescription.id, { description: draftDescription });
                closeDescriptionEditor();
            })
            .fail(() => showToast("danger", "Unable to update the project description.", "Update failed"))
            .always(() => setIsSavingDescription(false));
    }

    return (
        <div className="project-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">Project administration</h1>
                        <p className="text-secondary mb-0">Manage software project details.</p>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        <span className="text-secondary small">{projects.length} project{projects.length === 1 ? "" : "s"}</span>
                        {auth.currentUser.role === "ADMIN" && (
                            <button
                                className="btn btn-primary"
                                data-bs-target={`#${createModalId}`}
                                data-bs-toggle="modal"
                                type="button"
                            >
                                Create new +
                            </button>
                        )}
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
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="3">Loading projects...</td></tr>
                                ) : projects.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="3">No projects found.</td></tr>
                                ) : projects.map((project) => (
                                    <tr key={project.id}>
                                        <td className="small text-break">{project.id}</td>
                                        <td><EditableName onNameSaved={(id, name) => updateProject(id, { name })} project={project} /></td>
                                        <td>
                                            <div className="project-description mb-2">{project.description || "No description"}</div>
                                            <button className="btn btn-outline-secondary btn-sm" onClick={() => openDescriptionEditor(project)} type="button">
                                                <i aria-hidden="true" className="bi bi-pencil me-1" />Edit description
                                            </button>
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
                    <h2 className="modal-title fs-5">Edit project description</h2>
                </div>
                <div className="modal-body d-flex flex-column">
                    <label className="form-label" htmlFor="project-description">Description</label>
                    <textarea
                        className="form-control flex-grow-1"
                        disabled={isSavingDescription}
                        id="project-description"
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
            <Modal id={createModalId}>
                <CreateProjectForm onCreated={handleProjectCreated} />
            </Modal>
        </div>
    );
}
