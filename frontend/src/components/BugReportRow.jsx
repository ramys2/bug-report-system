import { Link } from "react-router";
import "./BugReportRow.css";
import { formatDateTime } from "../utils/date";

/**
 * One report as a card row: title (a router link to `/reports/{reportId}`), author, status, severity and creation time.
 *
 * @param {object} props
 * @param {import("../api/bug-report.js").BugReportBrief} props.report the report to show
 */
function BugReportRow({ report }) {
    return (
        <article className="border rounded-4 px-3 py-3">
            <div className="row g-3 align-items-center">
                <div className="col-12 col-md-3">
                    <Link to={`/reports/${report.reportId}`} className="link-dark fw-semibold">
                        {report.title}
                    </Link>
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">Author</span>
                    {report.author}
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">Status</span>
                    {report.status}
                </div>
                <div className="col-6 col-md-2">
                    <span className="d-md-none d-block small text-secondary">Severity</span>
                    {report.severity}
                </div>
                <div className="col-6 col-md-3">
                    <span className="d-md-none d-block small text-secondary">Created at</span>
                    {formatDateTime(report.createdAt)}
                </div>
            </div>
        </article>
    );
}

export default BugReportRow;
