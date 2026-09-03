import { useContext, useEffect, useState } from "react";
import { useParams } from "react-router";
import {
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

function displayValue(value) {
    return value || "Not provided";
}

function ReportDetail({ label, value }) {
    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">{displayValue(value)}</dd>
        </div>
    );
}

function EditableReportSection({
    title,
    reportId,
    value,
    updateValue,
    onValueSaved,
}) {
    const [isEditing, setIsEditing] = useState(false);
    const [draftValue, setDraftValue] = useState("");
    const [isSaving, setIsSaving] = useState(false);

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
            .fail(() => alert(`Unable to update ${title.toLowerCase()}!`))
            .always(() => setIsSaving(false));
    }

    return (
        <section className="border rounded-4 p-3 h-100">
            <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">{title}</h2>
                {!isEditing && (
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
            {isEditing ? (
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

const STATUS_OPTIONS = [
    "OPEN",
    "ASSIGNED",
    "IN_PROGRESS",
    "NEEDS_INFORMATION",
    "REVIEWING",
    "REJECTED",
    "CLOSED",
];

const SEVERITY_OPTIONS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

function EditableSelectField({
    label,
    reportId,
    value,
    options,
    placeholder,
    getOptionValue,
    getOptionLabel,
    updateValue,
    onValueSaved,
}) {
    const [isEditing, setIsEditing] = useState(false);
    const [selectedOptionValue, setSelectedOptionValue] = useState("");
    const [isSaving, setIsSaving] = useState(false);

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
            .fail(() => alert(`Unable to update ${label.toLowerCase()}!`))
            .always(() => setIsSaving(false));
    }

    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">
                {isEditing ? (
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
                        disabled={options.length === 0}
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

export default function BugReportPage() {
    const { id } = useParams();
    const { currentUser } = useContext(AuthContext);
    const [bugReport, setBugReport] = useState(null);
    const [comments, setComments] = useState([]);
    const [commentDraft, setCommentDraft] = useState("");
    const [isCommentEditing, setIsCommentEditing] = useState(false);
    const [isCommentSaving, setIsCommentSaving] = useState(false);
    const [removingCommentId, setRemovingCommentId] = useState(null);

    const [developers, setDevelopers] = useState([]);
    const [projects, setProjects] = useState([]);
    const [components, setComponents] = useState([]);

    useEffect(() => {
        getReport(id)
            .done((data) => setBugReport(data))
            .fail(() => alert("Unable to fetch bug report!"));

        getComments(id)
            .done((data) => setComments(data))
            .fail(() => alert("Unable to fetch comments!"));

        getDevelopers()
            .done(setDevelopers)
            .fail(() => alert("Failed to fetch developers."));

        getProjects()
            .done(setProjects)
            .fail(() => alert("Failed to fetch projects."));

        getComponents()
            .done(setComponents)
            .fail(() => alert("Failed to fetch components."));
    }, [id]);

    function saveComment() {
        setIsCommentSaving(true);

        createComment(id, { content: commentDraft })
            .done((comment) => {
                setComments((existingComments) => [comment, ...existingComments]);
                setCommentDraft("");
                setIsCommentEditing(false);
            })
            .fail(() => alert("Unable to save comment!"))
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
            .fail(() => alert("Unable to remove comment!"))
            .always(() => setRemovingCommentId(null));
    }

    return (
        <div className="bug-report-page d-flex flex-column">
            <main className="container flex-grow-1 py-4 text-start">
                {bugReport === null ? (
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
                                </section>
                            </div>
                            <div className="col-12 col-lg-7">
                                <EditableReportSection
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
                                            canRemove={comment.authorId === currentUser?.id || currentUser?.role === "ADMIN"}
                                            isCurrentUser={comment.authorId === currentUser?.id}
                                            key={comment.id}
                                            onRemove={removeCommentById}
                                            isRemoving={removingCommentId === comment.id}
                                        />
                                    ))}
                                </div>
                            )}
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
                        </section>
                    </>
                )}
            </main>
        </div>
    );
}

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
