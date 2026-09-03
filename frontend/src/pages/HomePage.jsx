import { useEffect, useState } from "react";
import { Modal as BootstrapModal } from "bootstrap";
import BugReportList from "../components/BugReportList";
import CreateBugReportForm from "../components/CreateBugReportForm";
import Modal from "../components/Modal";
import QuickFilters from "../components/QuickFilters";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";
import { getComponents, getDevelopers, getProjects } from "../api/create-bug-report-options";

const createBugReportModalId = "create-bug-report-modal";

function HomePage() {
    const [bugReports, setBugReports] = useState([]);
    const [isQuickFilterActive, setIsQuickFilterActive] = useState(false);
    const [developers, setDevelopers] = useState([]);
    const [projects, setProjects] = useState([]);
    const [components, setComponents] = useState([]);

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

    useEffect(() => {
        getDevelopers()
            .done(setDevelopers)
            .fail(() => alert("Failed to fetch developers."));

        getProjects()
            .done(setProjects)
            .fail(() => alert("Failed to fetch projects."));

        getComponents()
            .done(setComponents)
            .fail(() => alert("Failed to fetch components."));
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
