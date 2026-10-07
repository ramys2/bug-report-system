import { useContext, useEffect, useState } from "react";
import { getAccounts, updateAccountRole } from "../api/account";
import AuthContext from "../components/AuthContext";
import { showToast } from "../components/toast";
import "./UserAdminPage.css";

/**
 * Roles that can be chosen, same values as the backend enum.
 */
const ROLE_OPTIONS = ["ADMIN", "DEVELOPER", "REPORTER"];
/**
 * Number of users requested per page.
 */
const PAGE_SIZE = 10;
/**
 * How long the filters must stay unchanged before the users are requested again, so that typing does not send one request per key press.
 */
const FILTER_DELAY_MS = 300;
const NO_FILTERS = { id: "", name: "", email: "", role: "" };

/**
 * Page numbers to show in the pagination bar: the first, the last and the ones next to the current page.
 * A `null` entry stands for a gap ("…") between two numbers that are not neighbours.
 *
 * @param {number} currentPage the current page, counted from 1
 * @param {number} totalPages number of pages, at least 1
 * @returns {Array<number|null>} e.g. `[1, null, 5, 6, 7, null, 20]` for page 6 of 20
 */
function getVisiblePages(currentPage, totalPages) {
    const pages = [];

    for (let pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
        const isEdge = pageNumber === 1 || pageNumber === totalPages;
        const isNearCurrent = Math.abs(pageNumber - currentPage) <= 1;

        if (isEdge || isNearCurrent) {
            if (pages.length > 0 && pageNumber - pages.at(-1) > 1) {
                pages.push(null);
            }
            pages.push(pageNumber);
        }
    }

    return pages;
}

/**
 * Table cell that shows a user's role and lets an admin change it inline (edit, choose a role, Save or Cancel).
 * Save calls `PATCH /api/accounts/{id}/role`; on success `onRoleSaved` is called, on failure an error toast is shown (the backend refuses e.g. removing the last admin).
 *
 * @param {object} props
 * @param {{id: string, name: string, email: string, role: string}} props.user the account shown
 * @param {() => void} props.onRoleSaved called after the role was saved
 */
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
                onRoleSaved();
                setIsEditing(false);
            })
            .fail(() => showToast("danger", "Unable to update the user's role.", "Update failed"))
            .always(() => setIsSaving(false));
    }

    if (!isEditing) {
        return (
            <button
                aria-label={`Edit role for ${user.name}`}
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
                aria-label={`Role for ${user.name}`}
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
                aria-label={`Cancel role change for ${user.name}`}
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
 * Admin page at `/admin/users` (ADMIN only): all accounts in a table with role editing.
 *
 * Filtering by id, name, email and role and paging (10 per page) are done by the backend (`GET /api/accounts`);
 * the page requests one page again whenever the filters or the page number change. The text filters are applied
 * after a short pause in typing. The signed-in admin's own row is shown without the edit button, because the backend refuses
 * to change your own role.
 * Takes no props.
 */
export default function UserAdminPage() {
    const { currentUser } = useContext(AuthContext);
    // `filters` follow the inputs on every key press; `appliedFilters` are the ones last sent to the backend.
    const [filters, setFilters] = useState(NO_FILTERS);
    const [appliedFilters, setAppliedFilters] = useState(NO_FILTERS);
    // 1-based for the pagination bar; the backend counts pages from 0.
    const [page, setPage] = useState(1);
    // The last response: `{ items, page, size, totalElements, totalPages }`.
    const [result, setResult] = useState({ items: [], totalElements: 0, totalPages: 0 });
    const [isLoading, setIsLoading] = useState(true);
    // Raised after a role was saved, to load the current page again.
    const [reloadCount, setReloadCount] = useState(0);

    // Waits until typing pauses, then applies the filters and goes back to the first page.
    useEffect(() => {
        const timer = setTimeout(() => {
            setAppliedFilters(filters);
            setPage(1);
        }, FILTER_DELAY_MS);

        return () => clearTimeout(timer);
    }, [filters]);

    useEffect(() => {
        // Set by the cleanup below when a newer request replaced this one, so a slow old response cannot overwrite the new one.
        let isOutdated = false;

        getAccounts({ ...appliedFilters, page: page - 1, size: PAGE_SIZE })
            .done((data) => {
                if (isOutdated) {
                    return;
                }
                if (data.totalPages > 0 && page > data.totalPages) {
                    // The page no longer exists (e.g. its last user got another role while a role filter is active).
                    setPage(data.totalPages);
                    return;
                }
                setResult(data);
            })
            .fail(() => {
                if (!isOutdated) {
                    showToast("danger", "Unable to fetch user accounts.", "Unable to load users");
                }
            })
            .always(() => {
                if (!isOutdated) {
                    setIsLoading(false);
                }
            });

        return () => {
            isOutdated = true;
        };
    }, [appliedFilters, page, reloadCount]);

    const totalPages = Math.max(1, result.totalPages);

    function updateFilter(name, value) {
        setFilters((currentFilters) => ({ ...currentFilters, [name]: value }));
    }

    function clearFilters() {
        setFilters(NO_FILTERS);
    }

    function handleRoleSaved() {
        // The changed user may no longer match an active role filter, so load the page again instead of patching the row.
        setReloadCount((count) => count + 1);
    }

    return (
        <div className="user-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">User administration</h1>
                        <p className="text-secondary mb-0">Manage user roles and find accounts.</p>
                    </div>
                    <span className="text-secondary small">{result.totalElements} user{result.totalElements === 1 ? "" : "s"}</span>
                </div>

                <section className="border rounded-4 p-3 mb-3">
                    <div className="row g-3 align-items-end">
                        <div className="col-12 col-md-6 col-xl-3">
                            <label className="form-label" htmlFor="user-id-filter">ID</label>
                            <input className="form-control" id="user-id-filter" onChange={(event) => updateFilter("id", event.target.value)} value={filters.id} />
                        </div>
                        <div className="col-12 col-md-6 col-xl-3">
                            <label className="form-label" htmlFor="name-filter">Name</label>
                            <input className="form-control" id="name-filter" onChange={(event) => updateFilter("name", event.target.value)} value={filters.name} />
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
                                    <th scope="col">Name</th>
                                    <th scope="col">Email</th>
                                    <th scope="col">Role</th>
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="4">Loading users...</td></tr>
                                ) : result.items.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="4">No users match these filters.</td></tr>
                                ) : result.items.map((user) => (
                                    <tr key={user.id}>
                                        <td className="small text-break">{user.id}</td>
                                        <td>{user.name}</td>
                                        <td>{user.email}</td>
                                        <td>
                                            {user.id === currentUser?.id ? (
                                                <span className="text-secondary" title="You cannot change your own role.">{user.role} (you)</span>
                                            ) : (
                                                <EditableRole onRoleSaved={handleRoleSaved} user={user} />
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <nav aria-label="User pages" className="border-top p-3">
                        <ul className="pagination justify-content-center mb-0">
                            <li className={`page-item ${page === 1 ? "disabled" : ""}`}>
                                <button className="page-link" disabled={page === 1} onClick={() => setPage(page - 1)} type="button">Previous</button>
                            </li>
                            {getVisiblePages(page, totalPages).map((pageNumber, index) => (
                                pageNumber === null ? (
                                    <li className="page-item disabled" key={`gap-${index}`}>
                                        <span className="page-link">…</span>
                                    </li>
                                ) : (
                                    <li className={`page-item ${pageNumber === page ? "active" : ""}`} key={pageNumber}>
                                        <button aria-current={pageNumber === page ? "page" : undefined} className="page-link" onClick={() => setPage(pageNumber)} type="button">{pageNumber}</button>
                                    </li>
                                )
                            ))}
                            <li className={`page-item ${page === totalPages ? "disabled" : ""}`}>
                                <button className="page-link" disabled={page === totalPages} onClick={() => setPage(page + 1)} type="button">Next</button>
                            </li>
                        </ul>
                    </nav>
                </section>
            </main>
        </div>
    );
}
