import { useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import AuthContext from "../components/AuthContext";
import { getSupportRequests, updateSupportRequestStatus } from "../api/support";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "../components/toast";
import { formatDateTime } from "../utils/date";
import "./SupportAdminPage.css";

const STATUS_CLASSES = {
    PENDING: "text-bg-warning",
    APPROVED: "text-bg-success",
    REJECTED: "text-bg-secondary",
};

/**
 * Administrator page at /admin/support. Shows full request details and resolves pending requests.
 * Reloads server data after a decision or conflict; outdated loads are ignored on cleanup.
 * Route visibility is controlled by AdminRoute; the backend enforces authorization and all resolution rules.
 */
export default function SupportAdminPage() {
    const { t, i18n } = useTranslation();
    const { currentUser } = useContext(AuthContext);
    const [requests, setRequests] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [hasLoadFailed, setHasLoadFailed] = useState(false);
    const [resolution, setResolution] = useState(null);
    const [reloadCount, setReloadCount] = useState(0);

    useEffect(() => {
        let isCurrent = true;

        getSupportRequests()
            .done((data) => {
                if (isCurrent) {
                    setRequests(data);
                    setHasLoadFailed(false);
                }
            })
            .fail((xhr) => {
                if (isCurrent) {
                    setHasLoadFailed(true);
                    showToast("danger", getApiErrorMessage(xhr), i18n.t("admin.support.loadFailed"));
                }
            })
            .always(() => {
                if (isCurrent) {
                    setIsLoading(false);
                }
            });

        return () => {
            isCurrent = false;
        };
    }, [reloadCount, i18n]);

    function refreshRequests() {
        setIsLoading(true);
        setReloadCount((count) => count + 1);
    }

    function resolveRequest(request, status) {
        const isSelfApproval = status === "APPROVED" && request.requesterId === currentUser?.id;
        if (resolution !== null || isLoading || request.status !== "PENDING" || isSelfApproval) {
            return;
        }

        setResolution({ id: request.id, status });
        updateSupportRequestStatus(request.id, status)
            .done(() => {
                showToast("success", i18n.t(status === "APPROVED" ? "admin.support.approved" : "admin.support.rejected"));
                refreshRequests();
            })
            .fail((xhr) => {
                showToast("danger", getApiErrorMessage(xhr), i18n.t("admin.support.resolveFailed"));
                // Another administrator may have resolved/deleted it; load the authoritative state again.
                if (xhr.status === 409 || xhr.status === 404) {
                    refreshRequests();
                }
            })
            .always(() => setResolution(null));
    }

    return (
        <div className="support-admin-page d-flex flex-column overflow-hidden">
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start overflow-hidden">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h1 className="h3 mb-1">{t("admin.support.heading")}</h1>
                        <p className="text-secondary mb-0">{t("admin.support.description")}</p>
                    </div>
                    <div className="d-flex align-items-center gap-3">
                        {!isLoading && !hasLoadFailed && (
                            <span className="text-secondary small">{t("admin.support.count", { count: requests.length })}</span>
                        )}
                        <button className="btn btn-outline-secondary" disabled={isLoading || resolution !== null} onClick={refreshRequests} type="button">
                            {t("admin.support.refresh")}
                        </button>
                    </div>
                </div>

                <section aria-busy={isLoading || resolution !== null} className="border rounded-4 d-flex flex-column flex-grow-1 overflow-hidden">
                    <div className="table-responsive flex-grow-1 overflow-auto">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light sticky-top">
                                <tr>
                                    <th scope="col">{t("admin.support.requester")}</th>
                                    <th scope="col">{t("support.requestType")}</th>
                                    <th scope="col">{t("admin.support.details")}</th>
                                    <th scope="col">{t("reports.status")}</th>
                                    <th scope="col">{t("reports.createdAt")}</th>
                                    <th scope="col">{t("admin.support.resolvedAt")}</th>
                                    <th scope="col">{t("admin.support.actions")}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {isLoading ? (
                                    <tr><td className="text-secondary" colSpan="7">{t("admin.support.loading")}</td></tr>
                                ) : hasLoadFailed ? (
                                    <tr><td className="text-secondary" colSpan="7">{t("admin.support.loadFailed")}</td></tr>
                                ) : requests.length === 0 ? (
                                    <tr><td className="text-secondary" colSpan="7">{t("admin.support.empty")}</td></tr>
                                ) : requests.map((request) => (
                                    <tr key={request.id}>
                                        <td className="text-break">
                                            <div>{request.requesterName}</div>
                                            <div className="small text-secondary">{request.requesterId}</div>
                                        </td>
                                        <td>{t(`admin.support.types.${request.type}`, { defaultValue: request.type })}</td>
                                        <td className="support-request-details">
                                            {request.type === "ROLE_CHANGE" && (
                                                <div>{t("support.requestedRole")}: {t(`admin.roles.${request.requestedRole}`, { defaultValue: request.requestedRole })}</div>
                                            )}
                                            {request.description && (
                                                <div className="support-request-description text-break">{request.description}</div>
                                            )}
                                        </td>
                                        <td>
                                            <span className={`badge ${STATUS_CLASSES[request.status] ?? "text-bg-secondary"}`}>
                                                {t(`admin.support.statuses.${request.status}`, { defaultValue: request.status })}
                                            </span>
                                        </td>
                                        <td className="text-nowrap">{formatDateTime(request.createdAt)}</td>
                                        <td className="text-nowrap">{request.resolvedAt ? formatDateTime(request.resolvedAt) : t("common.notProvided")}</td>
                                        <td>
                                            {request.status === "PENDING" ? (
                                                <>
                                                    <div className="d-flex gap-2">
                                                        <button
                                                            aria-label={t("admin.support.approveFor", { name: request.requesterName })}
                                                            className="btn btn-outline-success btn-sm"
                                                            disabled={resolution !== null || request.requesterId === currentUser?.id}
                                                            onClick={() => resolveRequest(request, "APPROVED")}
                                                            type="button"
                                                        >
                                                            {resolution?.id === request.id && resolution.status === "APPROVED" ? t("admin.support.resolving") : t("admin.support.approve")}
                                                        </button>
                                                        <button
                                                            aria-label={t("admin.support.rejectFor", { name: request.requesterName })}
                                                            className="btn btn-outline-danger btn-sm"
                                                            disabled={resolution !== null}
                                                            onClick={() => resolveRequest(request, "REJECTED")}
                                                            type="button"
                                                        >
                                                            {resolution?.id === request.id && resolution.status === "REJECTED" ? t("admin.support.resolving") : t("admin.support.reject")}
                                                        </button>
                                                    </div>
                                                    {request.requesterId === currentUser?.id && (
                                                        <div className="small text-secondary mt-1">{t("admin.support.ownApprovalHint")}</div>
                                                    )}
                                                </>
                                            ) : (
                                                <span className="text-secondary">—</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </main>
        </div>
    );
}
