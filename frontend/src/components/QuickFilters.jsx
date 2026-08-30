import "./QuickFilters.css";

function QuickFilters({ onReportedByMe, onAssignedToMe, onReset, isFilterActive }) {
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

            {isFilterActive && (
                <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={onReset}
                >
                    <i aria-hidden="true" className="bi bi-x-lg me-1" />
                    Reset
                </button>
            )}
        </div>
    );
}

export default QuickFilters;
