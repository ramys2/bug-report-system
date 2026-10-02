import { useContext, useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { Modal as BootstrapModal } from "bootstrap";
import {
    closeReport,
    getReport,
    updateAssignee,
    updateActualBehavior,
    updateComponent,
    updateDescription,
    updateExpectedBehavior,
    updateProject,
    updateSeverity,
    updateStatus,
    updateStepsToReproduce,
} from "../api/bug-report";
import { createComment, getComments, removeComment } from "../api/comment";
import { formatDateTime } from "../utils/date";
import "./BugReportPage.css";
import { getComponents, getDevelopers, getProjects } from "../api/create-bug-report-options";
import AuthContext from "../components/AuthContext";
import Modal from "../components/Modal";
import { showToast } from "../components/toast";

/**
 * Element id of the modal with the "close issue" form.
 */
const closeBugReportModalId = "close-bug-report-modal";
/**
 * Element id of the modal that shows the resolution of a closed report.
 */
const resolutionModalId = "resolution-modal";
/**
 * Empty state of the "close issue" form.
 */
const EMPTY_RESOLUTION = {
    description: "",
    fixedVersion: "",
    commitUrl: "",
};

/**
 * Text shown for a value; empty or missing values become "Not provided".
 */
function displayValue(value) {
    return value || "Not provided";
}

/**
 * Read-only label and value pair (`<dt>`/`<dd>`) in the report's detail list.
 *
 * @param {object} props
 * @param {string} props.label
 * @param {string|null} [props.value] shown through `displayValue`
 */
function ReportDetail({ label, value }) {
    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">{displayValue(value)}</dd>
        </div>
    );
}

/**
 * Card with a heading and a long text (description, steps to reproduce, ...) that can be edited in place with a textarea.
 * Save calls `updateValue(reportId, text)` (one of the `PATCH /api/reports/{id}/...` functions); on success `onValueSaved` is called, on failure an error toast is shown.
 *
 * @param {object} props
 * @param {string} props.title heading, also used in the error message
 * @param {string} props.reportId id of the report being edited
 * @param {string|null} props.value current text
 * @param {boolean} [props.isEditable=true] `false` hides the edit button (used for closed reports)
 * @param {(reportId: string, value: string) => JQuery.jqXHR} props.updateValue api function that saves the new text
 * @param {(value: string) => void} props.onValueSaved called with the saved text
 */
function EditableReportSection({
    title,
    reportId,
    value,
    isEditable = true,
    updateValue,
    onValueSaved,
}) {
    const [isEditing, setIsEditing] = useState(false);
    const [draftValue, setDraftValue] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const isCurrentlyEditing = isEditing && isEditable;

    function startEditing() {
        setDraftValue(value ?? "");
        setIsEditing(true);
    }

    function cancelEditing() {
        setIsEditing(false);
    }

    function saveValue() {
        setIsSaving(true);

        updateValue(reportId, draftValue)
            .done(() => {
                onValueSaved(draftValue);
                setIsEditing(false);
            })
            .fail(() => showToast("danger", `Unable to update ${title.toLowerCase()}!`, "Update failed"))
            .always(() => setIsSaving(false));
    }

    return (
        <section className="border rounded-4 p-3 h-100">
            <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">{title}</h2>
                {!isCurrentlyEditing && isEditable && (
                    <button
                        aria-label={`Edit ${title.toLowerCase()}`}
                        className="btn btn-outline-secondary btn-sm"
                        onClick={startEditing}
                        type="button"
                    >
                        <i aria-hidden="true" className="bi bi-pencil" />
                    </button>
                )}
            </div>
            {isCurrentlyEditing ? (
                <div>
                    <textarea
                        aria-label={title}
                        className="form-control"
                        disabled={isSaving}
                        onChange={(event) => setDraftValue(event.target.value)}
                        rows="6"
                        value={draftValue}
                    />
                    <div className="d-flex gap-2 mt-2">
                        <button
                            className="btn btn-primary btn-sm"
                            disabled={isSaving}
                            onClick={saveValue}
                            type="button"
                        >
                            {isSaving ? "Saving..." : "Save"}
                        </button>
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            disabled={isSaving}
                            onClick={cancelEditing}
                            type="button"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            ) : (
                <div className="mb-0 text-break text-pre-wrap">{displayValue(value)}</div>
            )}
        </section>
    );
}

/**
 * Statuses that can be chosen in the page. `CLOSED` is missing on purpose: reports are closed through the resolution form.
 */
const STATUS_OPTIONS = [
    "OPEN",
    "ASSIGNED",
    "IN_PROGRESS",
    "NEEDS_INFORMATION",
    "REVIEWING",
    "REJECTED",
];

/**
 * Severities that can be chosen, same values as the backend enum.
 */
const SEVERITY_OPTIONS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

/**
 * Detail-list entry (`<dt>`/`<dd>`) whose value can be changed in place with a select box (assignee, severity, status, project, component).
 * Save calls `updateValue(reportId, selectedValue)` (one of the `PATCH /api/reports/{id}/...` functions). The current option is found by comparing
 * the displayed `value` with each option's label, since the report only carries names, not ids.
 *
 * @param {object} props
 * @param {string} props.label
 * @param {string} props.reportId id of the report being edited
 * @param {string|null} props.value currently displayed label
 * @param {Array} props.options choices to offer
 * @param {(option: any) => string} props.getOptionValue value sent to the backend for an option (e.g. an id)
 * @param {(option: any) => string} props.getOptionLabel text shown for an option
 * @param {string} [props.placeholder] if given, an empty first option is shown and nothing is preselected
 * @param {boolean} [props.isEditable=true] `false` disables editing (used for closed reports)
 * @param {(reportId: string, value: string) => JQuery.jqXHR} props.updateValue api function that saves the choice
 * @param {(label: string) => void} props.onValueSaved called with the label of the saved option
 */
function EditableSelectField({
    label,
    reportId,
    value,
    options,
    placeholder,
    getOptionValue,
    getOptionLabel,
    isEditable = true,
    updateValue,
    onValueSaved,
}) {
    const [isEditing, setIsEditing] = useState(false);
    const [selectedOptionValue, setSelectedOptionValue] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const isCurrentlyEditing = isEditing && isEditable;

    function startEditing() {
        const selectedOption = options.find((option) => getOptionLabel(option) === value)
            ?? (placeholder ? null : options[0]);
        setSelectedOptionValue(selectedOption ? getOptionValue(selectedOption) : "");
        setIsEditing(true);
    }

    function cancelEditing() {
        setIsEditing(false);
    }

    function saveValue() {
        const selectedOption = options.find(
            (option) => getOptionValue(option) === selectedOptionValue,
        );

        if (!selectedOption) {
            return;
        }

        setIsSaving(true);

        updateValue(reportId, selectedOptionValue)
            .done(() => {
                onValueSaved(getOptionLabel(selectedOption));
                setIsEditing(false);
            })
            .fail(() => showToast("danger", `Unable to update ${label.toLowerCase()}!`, "Update failed"))
            .always(() => setIsSaving(false));
    }

    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">
                {isCurrentlyEditing ? (
                    <div>
                        <select
                            aria-label={label}
                            className="form-select"
                            disabled={isSaving}
                            onChange={(event) => setSelectedOptionValue(event.target.value)}
                            value={selectedOptionValue}
                        >
                            {placeholder && <option disabled value="">{placeholder}</option>}
                            {options.map((option) => (
                                <option key={getOptionValue(option)} value={getOptionValue(option)}>
                                    {getOptionLabel(option)}
                                </option>
                            ))}
                        </select>
                        <div className="d-flex gap-2 mt-2">
                            <button
                                className="btn btn-primary btn-sm"
                                disabled={isSaving || !selectedOptionValue}
                                onClick={saveValue}
                                type="button"
                            >
                                {isSaving ? "Saving..." : "Save"}
                            </button>
                            <button
                                className="btn btn-outline-secondary btn-sm"
                                disabled={isSaving}
                                onClick={cancelEditing}
                                type="button"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                ) : (
                    <button
                        aria-label={`Edit ${label.toLowerCase()}`}
                        className="editable-enum-display"
                        disabled={!isEditable || options.length === 0}
                        onClick={startEditing}
                        type="button"
                    >
                        <span>{displayValue(value)}</span>
                        <i aria-hidden="true" className="bi bi-pencil" />
                    </button>
                )}
            </dd>
        </div>
    );
}

/**
 * Report detail page at `/reports/:id`.
 *
 * On load it fetches the report (`GET /api/reports/{id}`), its comments (`GET /api/reports/{id}/comments`) and the developers, projects and components
 * used by the select fields. It shows the report with in-place editing of assignee, severity, status, project, component, description,
 * steps to reproduce, expected and actual behavior, and a comment section (add: `POST .../comments`, remove: `DELETE /api/comments/{id}`).
 *
 * "Close issue" opens a modal that sends `POST /api/reports/{id}/resolution` after a confirmation dialog; afterwards the page treats the report as
 * closed (nothing is editable, no comments can be added or removed) and offers "Show resolution" instead. Whether a comment can be removed is
 * decided in the UI (own comments or ADMIN); the backend checks it again. Takes no props; the id comes from the URL.
 */
export default function BugReportPage() {
    const { id } = useParams();
    const { currentUser } = useContext(AuthContext);
    const [bugReport, setBugReport] = useState(null);
    const [loadError, setLoadError] = useState(false);
    const [comments, setComments] = useState([]);
    const [commentDraft, setCommentDraft] = useState("");
    const [isCommentEditing, setIsCommentEditing] = useState(false);
    const [isCommentSaving, setIsCommentSaving] = useState(false);
    const [removingCommentId, setRemovingCommentId] = useState(null);
    const [resolutionDraft, setResolutionDraft] = useState(EMPTY_RESOLUTION);
    const [isClosing, setIsClosing] = useState(false);

    const [developers, setDevelopers] = useState([]);
    const [projects, setProjects] = useState([]);
    const [components, setComponents] = useState([]);
    const isClosed = bugReport?.status === "CLOSED";

    useEffect(() => {
        getReport(id)
            .done((data) => {
                setBugReport(data);
                setLoadError(false);
            })
            .fail(() => {
                setLoadError(true);
                showToast("danger", "Unable to fetch bug report!", "Unable to load report");
            });

        getComments(id)
            .done((data) => setComments(data))
            .fail(() => showToast("danger", "Unable to fetch comments!", "Unable to load comments"));

        getDevelopers()
            .done(setDevelopers)
            .fail(() => showToast("danger", "Failed to fetch developers.", "Unable to load developers"));

        getProjects()
            .done(setProjects)
            .fail(() => showToast("danger", "Failed to fetch projects.", "Unable to load projects"));

        getComponents()
            .done(setComponents)
            .fail(() => showToast("danger", "Failed to fetch components.", "Unable to load components"));
    }, [id]);

    function saveComment() {
        setIsCommentSaving(true);

        createComment(id, { content: commentDraft })
            .done((comment) => {
                setComments((existingComments) => [comment, ...existingComments]);
                setCommentDraft("");
                setIsCommentEditing(false);
            })
            .fail(() => showToast("danger", "Unable to save comment!", "Comment not saved"))
            .always(() => setIsCommentSaving(false));
    }

    function removeCommentById(commentId) {
        setRemovingCommentId(commentId);

        removeComment(commentId)
            .done(() => {
                setComments((existingComments) => (
                    existingComments.filter((comment) => comment.id !== commentId)
                ));
            })
            .fail(() => showToast("danger", "Unable to remove comment!", "Comment not removed"))
            .always(() => setRemovingCommentId(null));
    }

    function updateResolutionDraft(field, value) {
        setResolutionDraft((resolution) => ({ ...resolution, [field]: value }));
    }

    function closeResolutionModal() {
        const modalElement = document.getElementById(closeBugReportModalId);

        if (modalElement) {
            BootstrapModal.getOrCreateInstance(modalElement).hide();
        }
    }

    function submitResolution(event) {
        event.preventDefault();

        if (!resolutionDraft.description.trim()) {
            showToast("warning", "Resolution description is required.", "Missing information");
            return;
        }

        const isConfirmed = window.confirm(
            "Are you sure you want to close this issue? It cannot be reopened.",
        );

        if (!isConfirmed) {
            return;
        }

        setIsClosing(true);

        closeReport(bugReport.id, resolutionDraft)
            .done(() => {
                setBugReport((report) => ({
                    ...report,
                    resolution: resolutionDraft,
                    status: "CLOSED",
                }));
                setResolutionDraft(EMPTY_RESOLUTION);
                closeResolutionModal();
            })
            .fail(() => showToast("danger", "Unable to close issue!", "Issue not closed"))
            .always(() => setIsClosing(false));
    }

    return (
        <div className="bug-report-page d-flex flex-column">
            <main className="container flex-grow-1 py-4 text-start">
                {loadError ? (
                    <div className="text-center py-5">
                        <p className="text-secondary">Unable to load this bug report.</p>
                        <Link to="/" className="btn btn-primary">
                            Back to home
                        </Link>
                    </div>
                ) : bugReport === null ? (
                    <p className="text-secondary mb-0">Loading bug report...</p>
                ) : (
                    <>
                        <div className="row g-4 mb-4">
                            <div className="col-12 col-lg-5">
                                <section className="border rounded-4 p-3 h-100">
                                    <h1 className="h3 mb-1">{displayValue(bugReport.title)}</h1>
                                    <p className="text-secondary small mb-4">ID: {bugReport.id}</p>
                                    <dl className="row g-3 mb-0">
                                        <ReportDetail label="Reporter" value={bugReport.reporterName} />
                                        <EditableSelectField
                                            getOptionLabel={(developer) => developer.name}
                                            getOptionValue={(developer) => developer.id}
                                            isEditable={!isClosed}
                                            label="Assignee"
                                            onValueSaved={(assigneeName) => setBugReport((report) => ({
                                                ...report,
                                                assigneeName,
                                            }))}
                                            options={developers}
                                            placeholder="Select an assignee"
                                            reportId={bugReport.id}
                                            updateValue={updateAssignee}
                                            value={bugReport.assigneeName}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(severity) => severity}
                                            getOptionValue={(severity) => severity}
                                            isEditable={!isClosed}
                                            label="Severity"
                                            onValueSaved={(severity) => setBugReport((report) => ({ ...report, severity }))}
                                            options={SEVERITY_OPTIONS}
                                            reportId={bugReport.id}
                                            updateValue={updateSeverity}
                                            value={bugReport.severity}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(status) => status}
                                            getOptionValue={(status) => status}
                                            isEditable={!isClosed}
                                            label="Status"
                                            onValueSaved={(status) => setBugReport((report) => ({ ...report, status }))}
                                            options={STATUS_OPTIONS}
                                            reportId={bugReport.id}
                                            updateValue={updateStatus}
                                            value={bugReport.status}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(project) => project.name}
                                            getOptionValue={(project) => project.id}
                                            isEditable={!isClosed}
                                            label="Project"
                                            onValueSaved={(projectName) => setBugReport((report) => ({
                                                ...report,
                                                projectName,
                                            }))}
                                            options={projects}
                                            reportId={bugReport.id}
                                            updateValue={updateProject}
                                            value={bugReport.projectName}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(component) => component.name}
                                            getOptionValue={(component) => component.id}
                                            isEditable={!isClosed}
                                            label="Component"
                                            onValueSaved={(componentName) => setBugReport((report) => ({
                                                ...report,
                                                componentName,
                                            }))}
                                            options={components}
                                            reportId={bugReport.id}
                                            updateValue={updateComponent}
                                            value={bugReport.componentName}
                                        />
                                        <ReportDetail label="Created at" value={formatDateTime(bugReport.createdAt)} />
                                        <ReportDetail label="Updated at" value={formatDateTime(bugReport.updatedAt)} />
                                    </dl>
                                    <div className="border-top mt-4 pt-3">
                                        {bugReport.resolution ? (
                                            <button
                                                className="btn btn-outline-primary w-100"
                                                data-bs-target={`#${resolutionModalId}`}
                                                data-bs-toggle="modal"
                                                type="button"
                                            >
                                                Show resolution
                                            </button>
                                        ) : (
                                            <button
                                                className="btn btn-danger w-100"
                                                data-bs-target={`#${closeBugReportModalId}`}
                                                data-bs-toggle="modal"
                                                type="button"
                                            >
                                                Close issue
                                            </button>
                                        )}
                                    </div>
                                </section>
                            </div>
                            <div className="col-12 col-lg-7">
                                <EditableReportSection
                                    isEditable={!isClosed}
                                    onValueSaved={(description) => setBugReport((report) => ({
                                        ...report,
                                        description,
                                    }))}
                                    reportId={bugReport.id}
                                    title="Description"
                                    updateValue={updateDescription}
                                    value={bugReport.description}
                                />
                            </div>
                        </div>

                        <div className="row g-3 mb-3">
                            <div className="col-12">
                                <EditableReportSection
                                    isEditable={!isClosed}
                                    onValueSaved={(stepsToReproduce) => setBugReport((report) => ({
                                        ...report,
                                        stepsToReproduce,
                                    }))}
                                    reportId={bugReport.id}
                                    title="Steps to reproduce"
                                    updateValue={updateStepsToReproduce}
                                    value={bugReport.stepsToReproduce}
                                />
                            </div>
                            <div className="col-12 col-lg-6">
                                <EditableReportSection
                                    isEditable={!isClosed}
                                    onValueSaved={(expectedBehavior) => setBugReport((report) => ({
                                        ...report,
                                        expectedBehavior,
                                    }))}
                                    reportId={bugReport.id}
                                    title="Expected behavior"
                                    updateValue={updateExpectedBehavior}
                                    value={bugReport.expectedBehavior}
                                />
                            </div>
                            <div className="col-12 col-lg-6">
                                <EditableReportSection
                                    isEditable={!isClosed}
                                    onValueSaved={(actualBehavior) => setBugReport((report) => ({
                                        ...report,
                                        actualBehavior,
                                    }))}
                                    reportId={bugReport.id}
                                    title="Actual behavior"
                                    updateValue={updateActualBehavior}
                                    value={bugReport.actualBehavior}
                                />
                            </div>
                        </div>

                        <section className="border-top pt-4 mt-4">
                            <h2 className="h4 mb-3">Comments</h2>
                            {comments.length === 0 ? (
                                <p className="text-secondary">No comments yet.</p>
                            ) : (
                                <div className="d-flex flex-column gap-3">
                                    {comments.map((comment) => (
                                        <CommentCard
                                            comment={comment}
                                            canRemove={!isClosed && (comment.authorId === currentUser?.id || currentUser?.role === "ADMIN")}
                                            isCurrentUser={comment.authorId === currentUser?.id}
                                            key={comment.id}
                                            onRemove={removeCommentById}
                                            isRemoving={removingCommentId === comment.id}
                                        />
                                    ))}
                                </div>
                            )}
                            {!isClosed && (
                                <div className="mt-3">
                                    <textarea
                                        aria-label="New comment"
                                        className="form-control"
                                        disabled={isCommentSaving}
                                        onChange={(event) => setCommentDraft(event.target.value)}
                                        onFocus={() => setIsCommentEditing(true)}
                                        placeholder="Write a comment..."
                                        rows="4"
                                        value={commentDraft}
                                    />
                                    {isCommentEditing && (
                                        <div className="d-flex gap-2 mt-2">
                                            <button
                                                className="btn btn-primary btn-sm"
                                                disabled={isCommentSaving}
                                                onClick={saveComment}
                                                type="button"
                                            >
                                                {isCommentSaving ? "Saving..." : "Save"}
                                            </button>
                                            <button
                                                className="btn btn-outline-secondary btn-sm"
                                                disabled={isCommentSaving}
                                                onClick={() => {
                                                    setCommentDraft("");
                                                    setIsCommentEditing(false);
                                                }}
                                                type="button"
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}
                        </section>

                        <Modal id={closeBugReportModalId}>
                            <form onSubmit={submitResolution}>
                                <div className="modal-header">
                                    <h2 className="modal-title fs-5">Close issue</h2>
                                    <button
                                        aria-label="Close"
                                        className="btn-close"
                                        data-bs-dismiss="modal"
                                        disabled={isClosing}
                                        type="button"
                                    />
                                </div>
                                <div className="modal-body overflow-auto">
                                    <div className="mb-3">
                                        <label className="form-label" htmlFor="resolution-description">
                                            Resolution description
                                        </label>
                                        <textarea
                                            className="form-control"
                                            disabled={isClosing}
                                            id="resolution-description"
                                            onChange={(event) => updateResolutionDraft("description", event.target.value)}
                                            required
                                            rows="5"
                                            value={resolutionDraft.description}
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <label className="form-label" htmlFor="fixed-version">
                                            Fixed version
                                        </label>
                                        <input
                                            className="form-control"
                                            disabled={isClosing}
                                            id="fixed-version"
                                            onChange={(event) => updateResolutionDraft("fixedVersion", event.target.value)}
                                            value={resolutionDraft.fixedVersion}
                                        />
                                    </div>
                                    <div>
                                        <label className="form-label" htmlFor="commit-url">
                                            Commit URL
                                        </label>
                                        <input
                                            className="form-control"
                                            disabled={isClosing}
                                            id="commit-url"
                                            onChange={(event) => updateResolutionDraft("commitUrl", event.target.value)}
                                            value={resolutionDraft.commitUrl}
                                        />
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button
                                        className="btn btn-secondary"
                                        data-bs-dismiss="modal"
                                        disabled={isClosing}
                                        type="button"
                                    >
                                        Cancel
                                    </button>
                                    <button className="btn btn-danger" disabled={isClosing} type="submit">
                                        {isClosing ? "Closing..." : "Close issue"}
                                    </button>
                                </div>
                            </form>
                        </Modal>

                        <Modal fullscreen={false} id={resolutionModalId}>
                            <div className="modal-header">
                                <h2 className="modal-title fs-5">Resolution</h2>
                                <button
                                    aria-label="Close"
                                    className="btn-close"
                                    data-bs-dismiss="modal"
                                    type="button"
                                />
                            </div>
                            <div className="modal-body">
                                <dl className="row g-3 mb-0">
                                    <ReportDetail
                                        label="Description"
                                        value={bugReport.resolution?.description}
                                    />
                                    <ReportDetail
                                        label="Fixed version"
                                        value={bugReport.resolution?.fixedVersion}
                                    />
                                    <ReportDetail
                                        label="Commit URL"
                                        value={bugReport.resolution?.commitUrl}
                                    />
                                </dl>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-secondary" data-bs-dismiss="modal" type="button">
                                    Close
                                </button>
                            </div>
                        </Modal>
                    </>
                )}
            </main>
        </div>
    );
}

/**
 * One comment as a card: author, time and text. The current user's comments are aligned right and get a different style.
 *
 * @param {object} props
 * @param {{id: string, authorId: string, authorName: string, content: string, createdAt: string}} props.comment
 * @param {boolean} props.canRemove whether to show the remove button
 * @param {boolean} props.isCurrentUser whether the signed-in user wrote the comment
 * @param {boolean} props.isRemoving whether a removal is in progress (disables the button)
 * @param {(commentId: string) => void} props.onRemove called with the comment id when the button is clicked
 */
function CommentCard({ canRemove, comment, isCurrentUser, isRemoving, onRemove }) {
    return (
        <div className="row">
            <div className={`col-12 col-md-6 ${isCurrentUser ? "ms-md-auto" : ""}`}>
                <article className={`comment-card border rounded-4 p-3 ${isCurrentUser ? "comment-card-current-user" : ""}`}>
                    <div className="d-flex justify-content-between gap-3 mb-2">
                        <h3 className="h6 mb-0">
                            {displayValue(comment.authorName)}
                        </h3>
                        <div className="comment-card-actions d-flex flex-column align-items-end">
                            <time className="small text-secondary text-nowrap" dateTime={comment.createdAt}>
                                {formatDateTime(comment.createdAt)}
                            </time>
                            {canRemove && (
                                <button
                                    aria-label="Remove comment"
                                    className="comment-card-remove btn btn-danger btn-sm"
                                    disabled={isRemoving}
                                    onClick={() => onRemove(comment.id)}
                                    type="button"
                                >
                                    <i aria-hidden="true" className="bi bi-x-lg" />
                                </button>
                            )}
                        </div>
                    </div>
                    <p className="mb-0 text-break text-pre-wrap">{comment.content}</p>
                </article>
            </div>
        </div>
    );
}
