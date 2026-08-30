import { useEffect, useState } from "react";
import { Modal as BootstrapModal } from "bootstrap";
import BugReportList from "../components/BugReportList";
import CreateBugReportForm from "../components/CreateBugReportForm";
import Modal from "../components/Modal";
import Navbar from "../components/Navbar";
import QuickFilters from "../components/QuickFilters";
import { components, developers, projects } from "../mock-data/createBugReportOptions";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";

const createBugReportModalId = "create-bug-report-modal";

function HomePage() {
    const [bugReports, setBugReports] = useState([]);
    const [isQuickFilterActive, setIsQuickFilterActive] = useState(false);

    function loadAllReports() {
        getAllReports()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                alert("Failed to fetch bug reports!");
            });
    }

    useEffect(() => {
        loadAllReports();
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
        loadAllReports();
    }

    function onReportCreated() {
        setIsQuickFilterActive(false);
        loadAllReports();
        const modalElement = document.getElementById(createBugReportModalId);

        if (modalElement) {
            BootstrapModal.getOrCreateInstance(modalElement).hide();
        }
    }

    return (
        <div className="home-page d-flex flex-column">
            <Navbar />
            <main className="container d-flex flex-column flex-grow-1 py-4 text-start">
                <div className="d-flex justify-content-between align-items-start">
                    <QuickFilters
                        onReportedByMe={onReportedByMe}
                        onAssignedToMe={onAssignedToMe}
                        onReset={onReset}
                        isFilterActive={isQuickFilterActive}
                    />
                    <button
                        type="button"
                        className="btn btn-primary"
                        data-bs-toggle="modal"
                        data-bs-target={`#${createBugReportModalId}`}
                    >
                        Create new +
                    </button>
                </div>
                <BugReportList reports={bugReports} />
                <Modal id={createBugReportModalId}>
                    <CreateBugReportForm
                        developers={developers}
                        projects={projects}
                        components={components}
                        onCreated={onReportCreated}
                    />
                </Modal>
            </main>
        </div>
    );
}

export default HomePage;
