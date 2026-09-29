import { useEffect, useState } from "react";
import { createComponent } from "../api/component";
import { searchUsers } from "../api/account";
import { showToast } from "./toast";

/**
 * Empty form state for name and description; also used to reset the form after a successful submit.
 */
const initialFormValues = { name: "", description: "" };

/**
 * Form (meant to sit inside a `Modal`) for creating a component with `POST /api/components`.
 *
 * The responsible user is picked by searching users by name (`GET /api/accounts/users?search=...`); results of outdated searches are ignored.
 * Only the name is required by the form, but the backend needs a responsible user, so submitting without one is expected to fail and show the error toast (TODO(verify): not tested).
 * On success the form is cleared and `onCreated` is called.
 *
 * @param {object} props
 * @param {() => void} props.onCreated called after the component was created
 */
export default function CreateComponentForm({ onCreated }) {
    const [formValues, setFormValues] = useState(initialFormValues);
    const [responsibleUser, setResponsibleUser] = useState(null);
    const [search, setSearch] = useState("");
    const [users, setUsers] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

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
    }, [search]);

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

        if (!formValues.name.trim()) {
            showToast("warning", "Please enter a component name.", "Missing information");
            return;
        }

        setIsSubmitting(true);
        createComponent({
            ...formValues,
            name: formValues.name.trim(),
            responsibleUserId: responsibleUser ? responsibleUser.userId : null,
        })
            .done(() => {
                setFormValues(initialFormValues);
                setResponsibleUser(null);
                onCreated();
            })
            .fail(() => showToast("danger", "Failed to create the component. Please try again.", "Component not created"))
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="d-flex flex-column h-100" onSubmit={handleSubmit}>
            <div className="modal-header">
                <h1 className="modal-title fs-5">Create new component</h1>
                <button aria-label="Close" className="btn-close" data-bs-dismiss="modal" type="button" />
            </div>
            <div className="modal-body overflow-auto">
                <div className="mb-3">
                    <label className="form-label" htmlFor="new-component-name">Name *</label>
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
                    <label className="form-label" htmlFor="new-component-description">Description</label>
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
                    <label className="form-label" htmlFor="new-component-responsible-user">Responsible user</label>
                    {responsibleUser ? (
                        <div className="d-flex align-items-center gap-2">
                            <span>{responsibleUser.name}</span>
                            <button className="btn btn-outline-secondary btn-sm" onClick={() => setResponsibleUser(null)} type="button">Change</button>
                        </div>
                    ) : (
                        <>
                            <input
                                className="form-control"
                                disabled={isSubmitting}
                                id="new-component-responsible-user"
                                onChange={(event) => changeSearch(event.target.value)}
                                placeholder="Search users"
                                value={search}
                            />
                            {search.trim() && (
                                <div className="border rounded bg-white mt-1">
                                    {isSearching ? (
                                        <p className="text-secondary small m-2">Searching...</p>
                                    ) : users.length === 0 ? (
                                        <p className="text-secondary small m-2">No matching users.</p>
                                    ) : users.map((user) => (
                                        <button
                                            className="btn btn-light d-block text-start w-100 rounded-0"
                                            disabled={isSubmitting}
                                            key={user.userId}
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
                <button className="btn btn-secondary" data-bs-dismiss="modal" type="button">Cancel</button>
                <button className="btn btn-primary" disabled={isSubmitting} type="submit">
                    {isSubmitting ? "Creating..." : "Create component"}
                </button>
            </div>
        </form>
    );
}
