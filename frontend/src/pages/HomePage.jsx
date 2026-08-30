import { useEffect, useState } from "react";
import BugReportList from "../components/BugReportList";
import Navbar from "../components/Navbar";
import QuickFilters from "../components/QuickFilters";
// import bugReports from "../mock-data/bugReports";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";

function HomePage() {
    const [bugReports, setBugReports] = useState([]);

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
        getReported()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Couldn't fetch your reported items!");
            })
    }

    function onAssignedToMe() {
        getAssigned()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Couldn't fetch items assigned to you!");
            })
    }

    return (
        <div className="home-page d-flex flex-column">
            <Navbar />
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start">
                <QuickFilters onReportedByMe={onReportedByMe} onAssignedToMe={onAssignedToMe} />
                <BugReportList reports={bugReports} />
            </main>
        </div>
    );
}

export default HomePage;
