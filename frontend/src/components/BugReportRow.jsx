import "./BugReportRow.css";

function BugReportRow({ report }) {
    return (
        <article className="border rounded-4 px-3 py-3">
            <div className="row g-3 align-items-center">
                <div className="col-12 col-md-3">
                    <a href={`api/reports/${report.report_id}`} className="link-dark fw-semibold">
                        {report.title}
                    </a>
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
                    {report.created_at}
                </div>
            </div>
        </article>
    );
}

export default BugReportRow;
