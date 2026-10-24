import { useCallback, useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import Modal from "../components/Modal";
import AuthContext from "../components/AuthContext";
import CreateProjectForm from "../components/CreateProjectForm";
import EditableName from "../components/EditableName";
import EditDescriptionModal from "../components/EditDescriptionModal";
import { showToast } from "../components/toast";
import { getAllProjects, updateProjectDescription, updateProjectName } from "../api/project";
import "./ProjectAdminPage.css";

/**
 * Admin page at `/admin/projects` (ADMIN or DEVELOPER): all projects in a table with inline name editing and a modal for the description.
 *
 * Projects are loaded with `GET /api/projects`. Names and descriptions are saved with `PATCH /api/projects/{id}/name` and `/description`.
 * The "Create new" button (`POST /api/projects` through `CreateProjectForm`) is only shown to admins, matching the backend rule. Takes no props.
 */
export default function ProjectAdminPage() {
    const { t, i18n } = useTranslation();
    const auth = useContext(AuthContext);
    const [projects, setProjects] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [projectForDescription, setProjectForDescription] = useState(null);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    const loadProjects = useCallback(() => {
        return getAllProjects()
            .done(setProjects)
            .fail(() => showToast("danger", i18n.t("errors.loadProjects"), i18n.t("errors.loadProjectsTitle")))
            .always(() => setIsLoading(false));
    }, [i18n]);

    useEffect(() => {
        loadProjects();
    }, [loadProjects]);

    function updateProject(projectId, changes) {
        setProjects((currentProjects) => currentProjects.map((project) => (
            project.id === projectId ? { ...project, ...changes } : project
        )));
    }

    function handleProjectCreated() {
        loadProjects();
        setIsCreateOpen(false);
    }

    function handleDescriptionSaved(projectId, description) {
        updateProject(projectId, { description });
        setProjectForDescription(null);
    }

    return (
        <div className="project-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">{t("admin.project.heading")}</h1>
                        <p className="text-secondary mb-0">{t("admin.project.description")}</p>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        <span className="text-secondary small">{t("admin.project.count", { count: projects.length })}</span>
                        {auth.currentUser?.role === "ADMIN" && (
                            <button
                                className="btn btn-primary"
                                onClick={() => setIsCreateOpen(true)}
                                type="button"
                            >
                                {t("reports.createNew")}
                            </button>
                        )}
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
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="3">{t("admin.project.loading")}</td></tr>
                                ) : projects.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="3">{t("admin.project.empty")}</td></tr>
                                ) : projects.map((project) => (
                                    <tr key={project.id}>
                                        <td className="small text-break">{project.id}</td>
                                        <td>
                                            <EditableName
                                                id={project.id}
                                                label="project"
                                                name={project.name}
                                                onNameSaved={(id, name) => updateProject(id, { name })}
                                                onSave={updateProjectName}
                                            />
                                        </td>
                                        <td>
                                            <div className="project-description mb-2">{project.description || t("common.noDescription")}</div>
                                            <button className="btn btn-outline-secondary btn-sm" onClick={() => setProjectForDescription(project)} type="button">
                                                <i aria-hidden="true" className="bi bi-pencil me-1" />{t("common.editDescription")}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </main>

            <EditDescriptionModal
                item={projectForDescription}
                label="project"
                onHide={() => setProjectForDescription(null)}
                onSave={updateProjectDescription}
                onSaved={handleDescriptionSaved}
            />
            <Modal label={t("admin.project.createHeading")} onHide={() => setIsCreateOpen(false)} show={isCreateOpen}>
                <CreateProjectForm onCreated={handleProjectCreated} onHide={() => setIsCreateOpen(false)} />
            </Modal>
        </div>
    );
}
