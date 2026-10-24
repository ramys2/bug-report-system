import BugReportRow from "./BugReportRow";
import { useTranslation } from "react-i18next";
import "./BugReportList.css";

/**
 * Table-like list of reports with a header row (visible from the `md` breakpoint up) and one `BugReportRow` per report.
 *
 * @param {object} props
 * @param {import("../api/bug-report.js").BugReportBrief[]} props.reports reports as returned by `GET /api/reports` (or `/reported`, `/assigned`)
 */
function BugReportList({ reports }) {
    const { t } = useTranslation();

    return (
        <section aria-label={t("reports.listLabel")} className="bug-report-list border rounded-4 p-2 overflow-auto">
            <div className="d-none d-md-flex row g-0 px-3 py-2 fw-semibold">
                <div className="col-md-3">{t("reports.title")}</div>
                <div className="col-md-2">{t("reports.author")}</div>
                <div className="col-md-2">{t("reports.status")}</div>
                <div className="col-md-2">{t("reports.severity")}</div>
                <div className="col-md-3">{t("reports.createdAt")}</div>
            </div>

            <div className="d-flex flex-column gap-2">
                {reports.map((report) => (
                    <BugReportRow key={report.id} report={report} />
                ))}
            </div>
        </section>
    );
}

export default BugReportList;
