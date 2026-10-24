import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import "./BugReportRow.css";
import { formatDateTime } from "../utils/date";

/**
 * One report as a card row: title (a router link to `/reports/{id}`), reporter name, status, severity and creation time.
 *
 * @param {object} props
 * @param {import("../api/bug-report.js").BugReportBrief} props.report the report to show
 */
function BugReportRow({ report }) {
    const { t } = useTranslation();

    return (
        <article className="border rounded-4 px-3 py-3">
            <div className="row g-3 align-items-center">
                <div className="col-12 col-md-3">
                    <Link to={`/reports/${report.id}`} className="link-dark fw-semibold">
                        {report.title}
                    </Link>
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">{t("reports.author")}</span>
                    {report.reporterName}
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">{t("reports.status")}</span>
                    {t(`reports.statuses.${report.status}`, { defaultValue: report.status })}
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">{t("reports.severity")}</span>
                    {t(`reports.severities.${report.severity}`, { defaultValue: report.severity })}
                </div>
                <div className="col-6 col-md-3">
                    <span className="d-md-none d-block small text-secondary">{t("reports.createdAt")}</span>
                    {formatDateTime(report.createdAt)}
                </div>
            </div>
        </article>
    );
}

export default BugReportRow;
