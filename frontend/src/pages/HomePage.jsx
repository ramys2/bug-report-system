import { useEffect, useState } from "react";
import BugReportList from "../components/BugReportList";
import Navbar from "../components/Navbar";
import QuickFilters from "../components/QuickFilters";
// import bugReports from "../mock-data/bugReports";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";

function HomePage() {
    const [bugReports, setBugReports] = useState([]);
    const [isQuickFilterActive, setIsQuickFilterActive] = useState(false);

    useEffect(() => {
        getAllReports()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Failed to fetch bug reports!");
            })
    }, []);

    function onReportedByMe() {
        setIsQuickFilterActive(true);

        getReported()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Couldn't fetch your reported items!");
            })
    }

    function onAssignedToMe() {
        setIsQuickFilterActive(true);

        getAssigned()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Couldn't fetch items assigned to you!");
            })
    }

    function onReset() {
        setIsQuickFilterActive(false);

        getAllReports()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Failed to fetch bug reports!");
            })
    }

    return (
        <div className="home-page d-flex flex-column">
            <Navbar />
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start">
                <QuickFilters
                    onReportedByMe={onReportedByMe}
                    onAssignedToMe={onAssignedToMe}
                    onReset={onReset}
                    isFilterActive={isQuickFilterActive}
                />
                <BugReportList reports={bugReports} />
            </main>
        </div>
    );
}

export default HomePage;
