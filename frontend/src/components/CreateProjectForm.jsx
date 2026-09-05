import { useState } from "react";
import { createProject } from "../api/project";
import { showToast } from "./toast";

const initialFormValues = { name: "", description: "" };

export default function CreateProjectForm({ onCreated }) {
    const [formValues, setFormValues] = useState(initialFormValues);
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({ ...currentValues, [name]: value }));
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (!formValues.name.trim()) {
            showToast("warning", "Please enter a project name.", "Missing information");
            return;
        }

        setIsSubmitting(true);
        createProject({ ...formValues, name: formValues.name.trim() })
            .done(() => {
                setFormValues(initialFormValues);
                onCreated();
            })
            .fail(() => showToast("danger", "Failed to create the project. Please try again.", "Project not created"))
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">Create new project</h1>
                <button aria-label="Close" className="btn-close" data-bs-dismiss="modal" type="button" />
            </div>
            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-project-name">Name *</label>
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
                    <label className="form-label" htmlFor="new-project-description">Description</label>
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
                <button className="btn btn-secondary" data-bs-dismiss="modal" type="button">Cancel</button>
                <button className="btn btn-primary" disabled={isSubmitting} type="submit">
                    {isSubmitting ? "Creating..." : "Create project"}
                </button>
            </div>
        </form>
    );
}
