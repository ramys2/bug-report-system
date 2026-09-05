import { useEffect, useMemo, useState } from "react";
import { getAllAccounts, updateAccountRole } from "../api/account";
import { showToast } from "../components/toast";
import "./UserAdminPage.css";

const ROLE_OPTIONS = ["ADMIN", "DEVELOPER", "REPORTER"];
const PAGE_SIZE = 10;

function EditableRole({ user, onRoleSaved }) {
    const [isEditing, setIsEditing] = useState(false);
    const [draftRole, setDraftRole] = useState(user.role);
    const [isSaving, setIsSaving] = useState(false);

    function startEditing() {
        setDraftRole(user.role);
        setIsEditing(true);
    }

    function saveRole() {
        setIsSaving(true);

        updateAccountRole(user.id, draftRole)
            .done(() => {
                onRoleSaved(user.id, draftRole);
                setIsEditing(false);
            })
            .fail(() => showToast("danger", "Unable to update the user's role.", "Update failed"))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <button
                aria-label={`Edit role for ${user.username}`}
                className="btn btn-outline-secondary btn-sm"
                onClick={startEditing}
                type="button"
            >
                <span>{user.role}</span>
                <i aria-hidden="true" className="bi bi-pencil ms-2" />
            </button>
        );
    }

    return (
        <div className="d-flex align-items-center gap-2">
            <select
                aria-label={`Role for ${user.username}`}
                className="form-select form-select-sm"
                disabled={isSaving}
                onChange={(event) => setDraftRole(event.target.value)}
                value={draftRole}
            >
                {ROLE_OPTIONS.map((role) => (
                    <option key={role} value={role}>{role}</option>
                ))}
            </select>
            <button
                className="btn btn-primary btn-sm"
                disabled={isSaving || draftRole === user.role}
                onClick={saveRole}
                type="button"
            >
                {isSaving ? "Saving..." : "Save"}
            </button>
            <button
                aria-label={`Cancel role change for ${user.username}`}
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

export default function UserAdminPage() {
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [filters, setFilters] = useState({ id: "", username: "", email: "", role: "" });
    const [page, setPage] = useState(1);

    useEffect(() => {
        getAllAccounts()
            .done(setUsers)
            .fail(() => showToast("danger", "Unable to fetch user accounts.", "Unable to load users"))
            .always(() => setIsLoading(false));
    }, []);

    const filteredUsers = useMemo(() => {
        const normalizedFilters = Object.fromEntries(
            Object.entries(filters).map(([key, value]) => [key, value.trim().toLowerCase()]),
        );

        return users.filter((user) => (
            user.id.toLowerCase().includes(normalizedFilters.id)
            && user.username.toLowerCase().includes(normalizedFilters.username)
            && user.email.toLowerCase().includes(normalizedFilters.email)
            && (!normalizedFilters.role || user.role.toLowerCase() === normalizedFilters.role)
        ));
    }, [filters, users]);

    const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
    const currentPage = Math.min(page, totalPages);
    const visibleUsers = filteredUsers.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

    function updateFilter(name, value) {
        setFilters((currentFilters) => ({ ...currentFilters, [name]: value }));
        setPage(1);
    }

    function clearFilters() {
        setFilters({ id: "", username: "", email: "", role: "" });
        setPage(1);
    }

    function handleRoleSaved(userId, role) {
        setUsers((currentUsers) => currentUsers.map((user) => (
            user.id === userId ? { ...user, role } : user
        )));
    }

    return (
        <div className="user-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">User administration</h1>
                        <p className="text-secondary mb-0">Manage user roles and find accounts.</p>
                    </div>
                    <span className="text-secondary small">{filteredUsers.length} user{filteredUsers.length === 1 ? "" : "s"}</span>
                </div>

                <section className="border rounded-4 p-3 mb-3">
                    <div className="row g-3 align-items-end">
                        <div className="col-12 col-md-6 col-xl-3">
                            <label className="form-label" htmlFor="user-id-filter">ID</label>
                            <input className="form-control" id="user-id-filter" onChange={(event) => updateFilter("id", event.target.value)} value={filters.id} />
                        </div>
                        <div className="col-12 col-md-6 col-xl-3">
                            <label className="form-label" htmlFor="username-filter">Username</label>
                            <input className="form-control" id="username-filter" onChange={(event) => updateFilter("username", event.target.value)} value={filters.username} />
                        </div>
                        <div className="col-12 col-md-6 col-xl-3">
                            <label className="form-label" htmlFor="email-filter">Email</label>
                            <input className="form-control" id="email-filter" onChange={(event) => updateFilter("email", event.target.value)} value={filters.email} />
                        </div>
                        <div className="col-12 col-md-6 col-xl-2">
                            <label className="form-label" htmlFor="role-filter">Role</label>
                            <select className="form-select" id="role-filter" onChange={(event) => updateFilter("role", event.target.value)} value={filters.role}>
                                <option value="">All roles</option>
                                {ROLE_OPTIONS.map((role) => <option key={role} value={role}>{role}</option>)}
                            </select>
                        </div>
                        <div className="col-12 col-xl-1">
                            <button className="btn btn-outline-secondary w-100" onClick={clearFilters} type="button">Clear</button>
                        </div>
                    </div>
                </section>

                <section className="border rounded-4 d-flex flex-column flex-grow-1 overflow-hidden">
                    <div className="table-responsive flex-grow-1 overflow-auto">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light sticky-top">
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">Username</th>
                                    <th scope="col">Email</th>
                                    <th scope="col">Role</th>
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="4">Loading users...</td></tr>
                                ) : visibleUsers.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="4">No users match these filters.</td></tr>
                                ) : visibleUsers.map((user) => (
                                    <tr key={user.id}>
                                        <td className="small text-break">{user.id}</td>
                                        <td>{user.username}</td>
                                        <td>{user.email}</td>
                                        <td><EditableRole onRoleSaved={handleRoleSaved} user={user} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <nav aria-label="User pages" className="border-top p-3">
                        <ul className="pagination justify-content-center mb-0">
                            <li className={`page-item ${currentPage === 1 ? "disabled" : ""}`}>
                                <button className="page-link" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)} type="button">Previous</button>
                            </li>
                            {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                                <li className={`page-item ${pageNumber === currentPage ? "active" : ""}`} key={pageNumber}>
                                    <button aria-current={pageNumber === currentPage ? "page" : undefined} className="page-link" onClick={() => setPage(pageNumber)} type="button">{pageNumber}</button>
                                </li>
                            ))}
                            <li className={`page-item ${currentPage === totalPages ? "disabled" : ""}`}>
                                <button className="page-link" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)} type="button">Next</button>
                            </li>
                        </ul>
                    </nav>
                </section>
            </main>
        </div>
    );
}
