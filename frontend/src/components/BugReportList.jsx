import BugReportRow from "./BugReportRow";
import "./BugReportList.css";

function BugReportList({ reports }) {
    return (
        <section aria-label="Bug reports" className="bug-report-list border rounded-4 p-2 overflow-auto">
            <div className="d-none d-md-flex row g-0 px-3 py-2 fw-semibold">
                <div className="col-md-3">Title</div>
                <div className="col-md-2">Author</div>
                <div className="col-md-2">Status</div>
                <div className="col-md-2">Severity</div>
                <div className="col-md-3">Created at</div>
            </div>

            <div className="d-flex flex-column gap-2">
                {reports.map((report) => (
                    <BugReportRow key={report.reportId} report={report} />
                ))}
            </div>
        </section>
    );
}

export default BugReportList;
