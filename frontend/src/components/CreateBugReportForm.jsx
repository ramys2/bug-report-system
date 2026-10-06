import { useEffect, useState } from "react";
import { createReport } from "../api/bug-report";
import { getComponentsByProject } from "../api/create-bug-report-options";
import { showToast } from "./toast";

/**
 * Empty form state; also used to reset the form after a successful submit.
 */
const initialFormValues = {
    title: "",
    assigneeId: "",
    projectId: "",
    componentId: "",
    description: "",
    stepsToReproduce: "",
    expectedBehavior: "",
    actualBehavior: "",
    severity: ""
};

/**
 * Severity options offered in the form, same values as the backend enum.
 */
const severities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

/**
 * Form (meant to sit inside a `Modal`) for filing a new report with `POST /api/reports`.
 *
 * Title, project and severity are required; if one is missing a warning toast is shown and nothing is sent.
 * The component field is shown only once a project is picked. It is optional and always starts as "None". The components to choose from are those of the picked project
 * (`GET /api/components?projectId=...`); they are loaded again whenever the project changes, and results of outdated requests are ignored.
 * If loading fails an error toast is shown and "None" stays the only choice.
 * Empty optional fields (assignee, component, description, steps to reproduce, expected and actual behavior) are sent as `null`. On success the form is cleared and `onCreated` is called; on failure an error toast is shown.
 * The submit button is disabled while the request runs.
 *
 * @param {object} props
 * @param {{id: string, name: string}[]} props.developers users that can be chosen as assignee
 * @param {{id: string, name: string}[]} props.projects projects to choose from
 * @param {() => void} props.onCreated called after the report was created
 */
function CreateBugReportForm({ developers, projects, onCreated }) {
    const [formValues, setFormValues] = useState(initialFormValues);
    const [components, setComponents] = useState([]);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        if (!formValues.projectId) {
            return undefined;
        }

        let isCurrentProject = true;

        getComponentsByProject(formValues.projectId)
            .done((projectComponents) => {
                if (isCurrentProject) {
                    setComponents(projectComponents);
                }
            })
            .fail(() => {
                if (isCurrentProject) {
                    showToast("danger", "Failed to fetch components.", "Unable to load components");
                }
            });

        return () => {
            isCurrentProject = false;
        };
    }, [formValues.projectId]);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({
            ...currentValues,
            [name]: value
        }));
    }

    function handleProjectChange(event) {
        // The components belong to the project, so a new project clears the chosen component and the old list.
        setFormValues((currentValues) => ({
            ...currentValues,
            projectId: event.target.value,
            componentId: ""
        }));
        setComponents([]);
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (!formValues.title.trim() || !formValues.projectId || !formValues.severity) {
            showToast("warning", "Please fill in all required fields.", "Missing information");
            return;
        }

        setIsSubmitting(true);

        createReport({
            ...formValues,
            title: formValues.title.trim(),
            assigneeId: formValues.assigneeId || null,
            componentId: formValues.componentId || null,
            description: formValues.description || null,
            stepsToReproduce: formValues.stepsToReproduce || null,
            expectedBehavior: formValues.expectedBehavior || null,
            actualBehavior: formValues.actualBehavior || null
        })
            .done(() => {
                setFormValues(initialFormValues);
                setComponents([]);
                onCreated();
            })
            .fail(() => {
                showToast("danger", "Failed to create the bug report. Please try again.", "Report not created");
            })
            .always(() => {
                setIsSubmitting(false);
            });
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">Create new bug report</h1>
                <button
                    type="button"
                    className="btn-close"
                    data-bs-dismiss="modal"
                    aria-label="Close"
                />
            </div>

            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-title">Title *</label>
                    <input
                        id="bug-report-title"
                        name="title"
                        type="text"
                        className="form-control"
                        value={formValues.title}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-assignee">Assignee</label>
                    <select
                        id="bug-report-assignee"
                        name="assigneeId"
                        className="form-select"
                        value={formValues.assigneeId}
                        onChange={handleChange}
                    >
                        <option value="">Unassigned</option>
                        {developers.map((developer) => (
                            <option key={developer.id} value={developer.id}>
                                {developer.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-project">Project *</label>
                    <select
                        id="bug-report-project"
                        name="projectId"
                        className="form-select"
                        value={formValues.projectId}
                        onChange={handleProjectChange}
                        required
                    >
                        <option value="">Select a project</option>
                        {projects.map((project) => (
                            <option key={project.id} value={project.id}>
                                {project.name}
                            </option>
                        ))}
                    </select>
                </div>

                {formValues.projectId && (
                    <div className="mb-3">
                        <label className="form-label" htmlFor="bug-report-component">Component</label>
                        <select
                            id="bug-report-component"
                            name="componentId"
                            className="form-select"
                            value={formValues.componentId}
                            onChange={handleChange}
                        >
                            <option value="">None</option>
                            {components.map((component) => (
                                <option key={component.id} value={component.id}>
                                    {component.name}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-description">Description</label>
                    <textarea
                        id="bug-report-description"
                        name="description"
                        className="form-control"
                        rows="3"
                        value={formValues.description}
                        onChange={handleChange}
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-steps">Steps to reproduce</label>
                    <textarea
                        id="bug-report-steps"
                        name="stepsToReproduce"
                        className="form-control"
                        rows="3"
                        value={formValues.stepsToReproduce}
                        onChange={handleChange}
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-expected">Expected behavior</label>
                    <textarea
                        id="bug-report-expected"
                        name="expectedBehavior"
                        className="form-control"
                        rows="3"
                        value={formValues.expectedBehavior}
                        onChange={handleChange}
                    />
                </div>

                <div className="mb-3">
                    <label className="form-label" htmlFor="bug-report-actual">Actual behavior</label>
                    <textarea
                        id="bug-report-actual"
                        name="actualBehavior"
                        className="form-control"
                        rows="3"
                        value={formValues.actualBehavior}
                        onChange={handleChange}
                    />
                </div>

                <div>
                    <label className="form-label" htmlFor="bug-report-severity">Severity *</label>
                    <select
                        id="bug-report-severity"
                        name="severity"
                        className="form-select"
                        value={formValues.severity}
                        onChange={handleChange}
                        required
                    >
                        <option value="">Select severity</option>
                        {severities.map((severity) => (
                            <option key={severity} value={severity}>
                                {severity}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="modal-footer">
                <button type="button" className="btn btn-secondary" data-bs-dismiss="modal">
                    Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                    {isSubmitting ? "Creating..." : "Create bug report"}
                </button>
            </div>
        </form>
    );
}

export default CreateBugReportForm;
