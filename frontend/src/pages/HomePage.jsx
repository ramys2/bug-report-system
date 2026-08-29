import BugReportList from "../components/BugReportList";
import Navbar from "../components/Navbar";
import QuickFilters from "../components/QuickFilters";
import bugReports from "../mock-data/bugReports";
import "./HomePage.css";

function HomePage() {
    return (
        <div className="home-page d-flex flex-column">
            <Navbar />
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start">
                <QuickFilters />
                <BugReportList reports={bugReports} />
            </main>
        </div>
    );
}

export default HomePage;
