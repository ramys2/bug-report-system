import { useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import {
    closeReport,
    getReport,
    getStatusTransitions,
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
import { getDevelopers } from "../api/account";
import { getAllComponents } from "../api/component";
import { getAllProjects } from "../api/project";
import AuthContext from "../components/AuthContext";
import Modal from "../components/Modal";
import { showToast } from "../components/toast";

/**
 * Empty state of the "close issue" form.
 */
const EMPTY_RESOLUTION = {
    description: "",
    fixedVersion: "",
    commitUrl: "",
};

/**
 * Choice that stands for "no component" in the component select. `EditableSelectField` does not allow an empty option value,
 * so this has an id of its own, which `updateComponentOrRemove` turns into `null` for the backend.
 */
const NO_COMPONENT = { id: "none" };

/**
 * Saves the component choice with `PATCH /api/reports/{id}/component`; the `NO_COMPONENT` choice removes the component (`null`).
 */
function updateComponentOrRemove(reportId, componentId) {
    return updateComponent(reportId, componentId === NO_COMPONENT.id ? null : componentId);
}

/**
 * Text shown for a value; empty or missing values use the translated fallback.
 */
function displayValue(value, fallback) {
    return value || fallback;
}

/**
 * Read-only label and value pair (`<dt>`/`<dd>`) in the report's detail list.
 *
 * @param {object} props
 * @param {string} props.label
 * @param {string|null} [props.value] shown through `displayValue`
 */
function ReportDetail({ label, value }) {
    const { t } = useTranslation();

    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">{displayValue(value, t("common.notProvided"))}</dd>
        </div>
    );
}

/**
 * Card with a heading and a long text (description, steps to reproduce, ...) that can be edited in place with a textarea.
 * Save calls `updateValue(reportId, text)` (one of the `PATCH /api/reports/{id}/...` functions); on success `onValueSaved` is called, on failure an error toast is shown.
 *
 * @param {object} props
 * @param {string} props.titleKey translation key for the heading and error message field
 * @param {string} props.reportId id of the report being edited
 * @param {string|null} props.value current text
 * @param {boolean} [props.isEditable=true] `false` hides the edit button (used for closed reports)
 * @param {(reportId: string, value: string) => JQuery.jqXHR} props.updateValue api function that saves the new text
 * @param {(value: string) => void} props.onValueSaved called with the saved text
 */
function EditableReportSection({
    titleKey,
    reportId,
    value,
    isEditable = true,
    updateValue,
    onValueSaved,
}) {
    const { t, i18n } = useTranslation();
    const title = t(titleKey);
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
            .fail(() => showToast("danger", i18n.t("errors.updateField", { field: i18n.t(titleKey) }), i18n.t("errors.updateFailed")))
            .always(() => setIsSaving(false));
    }

    return (
        <section className="border rounded-4 p-3 h-100">
            <div className="d-flex justify-content-between align-items-center gap-3 mb-3">
                <h2 className="h5 mb-0">{title}</h2>
                {!isCurrentlyEditing && isEditable && (
                    <button
                        aria-label={t("common.editField", { field: title })}
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
                            {isSaving ? t("common.saving") : t("common.save")}
                        </button>
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            disabled={isSaving}
                            onClick={cancelEditing}
                            type="button"
                        >
                            {t("common.cancel")}
                        </button>
                    </div>
                </div>
            ) : (
                <div className="mb-0 text-break text-pre-wrap">{displayValue(value, t("common.notProvided"))}</div>
            )}
        </section>
    );
}

/**
 * Options for the status select: the current status followed by the statuses it can change to, as listed by
 * `GET /api/reports/status-transitions`. Empty if no change is possible, which disables editing.
 */
function statusOptions(statusTransitions, currentStatus) {
    const nextStatuses = statusTransitions[currentStatus] ?? [];
    return nextStatuses.length > 0 ? [currentStatus, ...nextStatuses] : [];
}

/**
 * Severities that can be chosen, same values as the backend enum.
 */
const SEVERITY_OPTIONS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

/**
 * Detail-list entry (`<dt>`/`<dd>`) whose value can be changed in place with a select box (assignee, severity, status, project, component).
 * Save calls `updateValue(reportId, selectedValue)` (one of the `PATCH /api/reports/{id}/...` functions). The current option is found by comparing
 * `currentValue` with each option's value (not the displayed label, because labels such as names can be duplicated).
 *
 * @param {object} props
 * @param {string} props.labelKey translation key for the field label
 * @param {string} props.reportId id of the report being edited
 * @param {string|null} props.value currently displayed label
 * @param {string|null} props.currentValue option value (e.g. an id) of the current choice; null if nothing is chosen
 * @param {Array} props.options choices to offer
 * @param {(option: any) => string} props.getOptionValue value sent to the backend for an option (e.g. an id)
 * @param {(option: any) => string} props.getOptionLabel text shown for an option
 * @param {string} [props.placeholder] if given, an empty first option is shown and nothing is preselected
 * @param {boolean} [props.isEditable=true] `false` disables editing (used for closed reports)
 * @param {() => void} [props.loadOptions] called when editing starts, for options that are fetched lazily; while it is given the edit button stays enabled even if `options` is still empty
 * @param {(reportId: string, value: string) => JQuery.jqXHR} props.updateValue api function that saves the choice
 * @param {(option: any) => void} props.onValueSaved called with the saved option
 */
function EditableSelectField({
    labelKey,
    reportId,
    value,
    currentValue,
    options,
    placeholder,
    getOptionValue,
    getOptionLabel,
    isEditable = true,
    loadOptions,
    updateValue,
    onValueSaved,
}) {
    const { t, i18n } = useTranslation();
    const label = t(labelKey);
    const [isEditing, setIsEditing] = useState(false);
    const [chosenOptionValue, setChosenOptionValue] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const isCurrentlyEditing = isEditing && isEditable;
    // Until the user picks something the current option is preselected. This is worked out on every render (not once in
    // startEditing) because lazily loaded options may arrive after editing has started.
    const defaultOption = options.find((option) => getOptionValue(option) === currentValue)
        ?? (placeholder ? null : options[0]);
    const selectedOptionValue = chosenOptionValue ?? (defaultOption ? getOptionValue(defaultOption) : "");

    function startEditing() {
        setChosenOptionValue(null);
        setIsEditing(true);
        loadOptions?.();
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
                onValueSaved(selectedOption);
                setIsEditing(false);
            })
            .fail(() => showToast("danger", i18n.t("errors.updateField", { field: i18n.t(labelKey) }), i18n.t("errors.updateFailed")))
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
                            onChange={(event) => setChosenOptionValue(event.target.value)}
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
                                {isSaving ? t("common.saving") : t("common.save")}
                            </button>
                            <button
                                className="btn btn-outline-secondary btn-sm"
                                disabled={isSaving}
                                onClick={cancelEditing}
                                type="button"
                            >
                                {t("common.cancel")}
                            </button>
                        </div>
                    </div>
                ) : (
                    <button
                        aria-label={t("common.editField", { field: label })}
                        className="editable-enum-display"
                        disabled={!isEditable || (options.length === 0 && !loadOptions)}
                        onClick={startEditing}
                        type="button"
                    >
                        <span>{displayValue(value, t("common.notProvided"))}</span>
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
 * On load it fetches the report (`GET /api/reports/{id}`), and its comments (`GET /api/comments?reportId={id}`). The developers and projects
 * used by the select fields are fetched when the assignee or project is first edited. The components offered are those of the report's project (`GET /api/components?projectId=...`), loaded again when the project is
 * changed; the backend then removes the report's component, so the page does the same and the component shows "None". It shows the report with in-place editing of assignee, severity, status, project, component, description,
 * steps to reproduce, expected and actual behavior, and a comment section (add: `POST /api/comments`, remove: `DELETE /api/comments/{id}`).
 *
 * "Close issue" opens a modal that sends `POST /api/reports/{id}/resolution` after a confirmation dialog; afterwards the page treats the report as
 * closed (nothing is editable, no comments can be added or removed) and offers "Show resolution" instead. Whether a comment can be removed is
 * decided in the UI (own comments or ADMIN); the backend checks it again. Takes no props; the id comes from the URL.
 */
export default function BugReportPage() {
    const { t, i18n } = useTranslation();
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
    const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);
    const [isResolutionModalOpen, setIsResolutionModalOpen] = useState(false);

    const [developers, setDevelopers] = useState([]);
    const [projects, setProjects] = useState([]);
    const [components, setComponents] = useState([]);
    const [statusTransitions, setStatusTransitions] = useState({});
    const isClosed = bugReport?.status === "CLOSED";
    const projectId = bugReport?.projectId;

    useEffect(() => {
        getReport(id)
            .done((data) => {
                setBugReport(data);
                setLoadError(false);
            })
            .fail(() => {
                setLoadError(true);
                showToast("danger", i18n.t("errors.loadReport"), i18n.t("errors.loadReportTitle"));
            });

        getComments(id)
            .done((data) => setComments(data))
            .fail(() => showToast("danger", i18n.t("errors.loadComments"), i18n.t("errors.loadCommentsTitle")));

        getStatusTransitions()
            .done(setStatusTransitions)
            .fail(() => showToast("danger", i18n.t("errors.loadStatuses"), i18n.t("errors.loadStatusesTitle")));
    }, [id, i18n]);

    function loadDevelopers() {
        // Fetched when the assignee is first edited; a failed request is retried the next time.
        if (developers.length === 0) {
            getDevelopers()
                .done(setDevelopers)
                .fail(() => showToast("danger", i18n.t("errors.loadDevelopers"), i18n.t("errors.loadDevelopersTitle")));
        }
    }

    function loadProjects() {
        if (projects.length === 0) {
            getAllProjects()
                .done(setProjects)
                .fail(() => showToast("danger", i18n.t("errors.loadProjects"), i18n.t("errors.loadProjectsTitle")));
        }
    }

    useEffect(() => {
        if (!projectId) {
            return undefined;
        }

        let isCurrentProject = true;

        getAllComponents(projectId)
            .done((projectComponents) => {
                if (isCurrentProject) {
                    setComponents(projectComponents);
                }
            })
            .fail(() => {
                if (isCurrentProject) {
                    showToast("danger", i18n.t("errors.loadComponents"), i18n.t("errors.loadComponentsTitle"));
                }
            });

        return () => {
            isCurrentProject = false;
        };
    }, [projectId, i18n]);

    function saveComment() {
        if (!commentDraft.trim()) {
            showToast("warning", t("reports.commentRequired"), t("common.missingInformation"));
            return;
        }

        setIsCommentSaving(true);

        createComment(id, { content: commentDraft })
            .done((comment) => {
                setComments((existingComments) => [comment, ...existingComments]);
                setCommentDraft("");
                setIsCommentEditing(false);
            })
            .fail(() => showToast("danger", i18n.t("errors.saveComment"), i18n.t("errors.saveCommentTitle")))
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
            .fail(() => showToast("danger", i18n.t("errors.removeComment"), i18n.t("errors.removeCommentTitle")))
            .always(() => setRemovingCommentId(null));
    }

    function updateResolutionDraft(field, value) {
        setResolutionDraft((resolution) => ({ ...resolution, [field]: value }));
    }

    function submitResolution(event) {
        event.preventDefault();

        if (!resolutionDraft.description.trim()) {
            showToast("warning", t("reports.resolutionRequired"), t("common.missingInformation"));
            return;
        }

        const isConfirmed = window.confirm(
            t("reports.confirmClose"),
        );

        if (!isConfirmed) {
            return;
        }

        setIsClosing(true);

        closeReport(bugReport.id, resolutionDraft)
            .done(() => {
                setResolutionDraft(EMPTY_RESOLUTION);
                setIsCloseModalOpen(false);
                getReport(bugReport.id)
                    .done(setBugReport)
                    .fail(() => showToast("danger", i18n.t("errors.refreshClosedReport"), i18n.t("errors.refreshReportTitle")));
            })
            .fail(() => showToast("danger", i18n.t("errors.closeReport"), i18n.t("errors.closeReportTitle")))
            .always(() => setIsClosing(false));
    }

    return (
        <div className="bug-report-page d-flex flex-column">
            <main className="container flex-grow-1 py-4 text-start">
                {loadError ? (
                    <div className="text-center py-5">
                        <p className="text-secondary">{t("reports.loadFailed")}</p>
                        <Link to="/" className="btn btn-primary">
                            {t("common.backToHome")}
                        </Link>
                    </div>
                ) : bugReport === null ? (
                    <p className="text-secondary mb-0">{t("reports.loading")}</p>
                ) : (
                    <>
                        <div className="row g-4 mb-4">
                            <div className="col-12 col-lg-5">
                                <section className="border rounded-4 p-3 h-100">
                                    <h1 className="h3 mb-1">{displayValue(bugReport.title, t("common.notProvided"))}</h1>
                                    <p className="text-secondary small mb-4">{t("common.idValue", { id: bugReport.id })}</p>
                                    <dl className="row g-3 mb-0">
                                        <ReportDetail label={t("reports.reporter")} value={bugReport.reporterName} />
                                        <EditableSelectField
                                            getOptionLabel={(developer) => developer.name}
                                            getOptionValue={(developer) => developer.id}
                                            isEditable={!isClosed}
                                            labelKey="reports.assignee"
                                            loadOptions={loadDevelopers}
                                            onValueSaved={(developer) => setBugReport((report) => ({
                                                ...report,
                                                assigneeId: developer.id,
                                                assigneeName: developer.name,
                                                status: report.status === "OPEN" ? "ASSIGNED" : report.status,
                                            }))}
                                            options={developers}
                                            placeholder={t("reports.selectAssignee")}
                                            reportId={bugReport.id}
                                            updateValue={updateAssignee}
                                            currentValue={bugReport.assigneeId}
                                            value={bugReport.assigneeName}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(severity) => t(`reports.severities.${severity}`, { defaultValue: severity })}
                                            getOptionValue={(severity) => severity}
                                            isEditable={!isClosed}
                                            labelKey="reports.severity"
                                            onValueSaved={(severity) => setBugReport((report) => ({ ...report, severity }))}
                                            options={SEVERITY_OPTIONS}
                                            reportId={bugReport.id}
                                            updateValue={updateSeverity}
                                            currentValue={bugReport.severity}
                                            value={t(`reports.severities.${bugReport.severity}`, { defaultValue: bugReport.severity })}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(status) => t(`reports.statuses.${status}`, { defaultValue: status })}
                                            getOptionValue={(status) => status}
                                            isEditable={!isClosed}
                                            labelKey="reports.status"
                                            onValueSaved={(status) => setBugReport((report) => ({ ...report, status }))}
                                            options={statusOptions(statusTransitions, bugReport.status)}
                                            reportId={bugReport.id}
                                            updateValue={updateStatus}
                                            currentValue={bugReport.status}
                                            value={t(`reports.statuses.${bugReport.status}`, { defaultValue: bugReport.status })}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(project) => project.name}
                                            getOptionValue={(project) => project.id}
                                            isEditable={!isClosed}
                                            labelKey="reports.project"
                                            loadOptions={loadProjects}
                                            onValueSaved={(project) => {
                                                if (project.id === bugReport.projectId) {
                                                    return; // same project: the backend keeps the component
                                                }

                                                setComponents([]);
                                                setBugReport((report) => ({
                                                    ...report,
                                                    projectId: project.id,
                                                    projectName: project.name,
                                                    componentId: null,
                                                    componentName: null,
                                                }));
                                            }}
                                            options={projects}
                                            reportId={bugReport.id}
                                            updateValue={updateProject}
                                            currentValue={bugReport.projectId}
                                            value={bugReport.projectName}
                                        />
                                        <EditableSelectField
                                            getOptionLabel={(component) => component === NO_COMPONENT ? t("common.none") : component.name}
                                            getOptionValue={(component) => component.id}
                                            isEditable={!isClosed}
                                            labelKey="reports.component"
                                            onValueSaved={(component) => setBugReport((report) => ({
                                                ...report,
                                                componentId: component === NO_COMPONENT ? null : component.id,
                                                componentName: component === NO_COMPONENT ? null : component.name,
                                            }))}
                                            options={[NO_COMPONENT, ...components]}
                                            reportId={bugReport.id}
                                            updateValue={updateComponentOrRemove}
                                            currentValue={bugReport.componentId ?? NO_COMPONENT.id}
                                            value={bugReport.componentName ?? t("common.none")}
                                        />
                                        <ReportDetail label={t("reports.createdAt")} value={formatDateTime(bugReport.createdAt)} />
                                        <ReportDetail label={t("reports.updatedAt")} value={formatDateTime(bugReport.updatedAt)} />
                                    </dl>
                                    <div className="border-top mt-4 pt-3">
                                        {bugReport.resolution ? (
                                            <button
                                                className="btn btn-outline-primary w-100"
                                                onClick={() => setIsResolutionModalOpen(true)}
                                                type="button"
                                            >
                                                {t("reports.showResolution")}
                                            </button>
                                        ) : (
                                            <button
                                                className="btn btn-danger w-100"
                                                onClick={() => setIsCloseModalOpen(true)}
                                                type="button"
                                            >
                                                {t("reports.closeIssue")}
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
                                    titleKey="reports.description"
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
                                    titleKey="reports.stepsToReproduce"
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
                                    titleKey="reports.expectedBehavior"
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
                                    titleKey="reports.actualBehavior"
                                    updateValue={updateActualBehavior}
                                    value={bugReport.actualBehavior}
                                />
                            </div>
                        </div>

                        <section className="border-top pt-4 mt-4">
                            <h2 className="h4 mb-3">{t("reports.comments")}</h2>
                            {comments.length === 0 ? (
                                <p className="text-secondary">{t("reports.noComments")}</p>
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
                                        aria-label={t("reports.newComment")}
                                        className="form-control"
                                        disabled={isCommentSaving}
                                        onChange={(event) => setCommentDraft(event.target.value)}
                                        onFocus={() => setIsCommentEditing(true)}
                                        placeholder={t("reports.commentPlaceholder")}
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
                                                {isCommentSaving ? t("common.saving") : t("common.save")}
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
                                                {t("common.cancel")}
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}
                        </section>

                        <Modal label={t("reports.closeIssue")} onHide={() => setIsCloseModalOpen(false)} show={isCloseModalOpen}>
                            <form onSubmit={submitResolution}>
                                <div className="modal-header">
                                    <h2 className="modal-title fs-5">{t("reports.closeIssue")}</h2>
                                    <button
                                        aria-label={t("common.close")}
                                        className="btn-close"
                                        onClick={() => setIsCloseModalOpen(false)}
                                        disabled={isClosing}
                                        type="button"
                                    />
                                </div>
                                <div className="modal-body overflow-auto">
                                    <div className="mb-3">
                                        <label className="form-label" htmlFor="resolution-description">
                                            {t("reports.resolutionDescription")}
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
                                            {t("reports.fixedVersion")}
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
                                            {t("reports.commitUrl")}
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
                                        onClick={() => setIsCloseModalOpen(false)}
                                        disabled={isClosing}
                                        type="button"
                                    >
                                        {t("common.cancel")}
                                    </button>
                                    <button className="btn btn-danger" disabled={isClosing} type="submit">
                                        {isClosing ? t("reports.closing") : t("reports.closeIssue")}
                                    </button>
                                </div>
                            </form>
                        </Modal>

                        <Modal label={t("reports.resolution")} fullscreen={false} onHide={() => setIsResolutionModalOpen(false)} show={isResolutionModalOpen}>
                            <div className="modal-header">
                                <h2 className="modal-title fs-5">{t("reports.resolution")}</h2>
                                <button
                                    aria-label={t("common.close")}
                                    className="btn-close"
                                    onClick={() => setIsResolutionModalOpen(false)}
                                    type="button"
                                />
                            </div>
                            <div className="modal-body">
                                <dl className="row g-3 mb-0">
                                    <ReportDetail
                                        label={t("reports.description")}
                                        value={bugReport.resolution?.description}
                                    />
                                    <ReportDetail
                                        label={t("reports.fixedVersion")}
                                        value={bugReport.resolution?.fixedVersion}
                                    />
                                    <ReportDetail
                                        label={t("reports.commitUrl")}
                                        value={bugReport.resolution?.commitUrl}
                                    />
                                </dl>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-secondary" onClick={() => setIsResolutionModalOpen(false)} type="button">
                                    {t("common.close")}
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
    const { t } = useTranslation();

    return (
        <div className="row">
            <div className={`col-12 col-md-6 ${isCurrentUser ? "ms-md-auto" : ""}`}>
                <article className={`comment-card border rounded-4 p-3 ${isCurrentUser ? "comment-card-current-user" : ""}`}>
                    <div className="d-flex justify-content-between gap-3 mb-2">
                        <h3 className="h6 mb-0">
                            {displayValue(comment.authorName, t("common.notProvided"))}
                        </h3>
                        <div className="comment-card-actions d-flex flex-column align-items-end">
                            <time className="small text-secondary text-nowrap" dateTime={comment.createdAt}>
                                {formatDateTime(comment.createdAt)}
                            </time>
                            {canRemove && (
                                <button
                                    aria-label={t("reports.removeComment")}
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
