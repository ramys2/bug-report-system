import "./QuickFilters.css";

function QuickFilters({ onReportedByMe, onAssignedToMe }) {
    return (
        <div className="d-flex align-items-center gap-3 mb-3">
            <span>Quick filters:</span>

            <button
                type="button"
                className="btn btn-link link-secondary p-0"
                onClick={onReportedByMe}
            >
                Reported by me
            </button>

            <button
                type="button"
                className="btn btn-link link-secondary p-0"
                onClick={onAssignedToMe}
            >
                Assigned to me
            </button>
        </div>
    );
}

export default QuickFilters;
