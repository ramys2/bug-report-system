import "./QuickFilters.css";

function QuickFilters() {
    return (
        <div className="d-flex align-items-center gap-3 mb-3">
            <span>Quick filters:</span>
            <a
                href="#reported-by-me"
                className="link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
            >
                Reported by me
            </a>
            <a
                href="#assigned-to-me"
                className="link-secondary link-underline-opacity-0 link-underline-opacity-100-hover"
            >
                Assigned to me
            </a>
        </div>
    );
}

export default QuickFilters;
