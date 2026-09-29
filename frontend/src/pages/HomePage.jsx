import { useEffect, useState } from "react";
import { Modal as BootstrapModal } from "bootstrap";
import BugReportList from "../components/BugReportList";
import CreateBugReportForm from "../components/CreateBugReportForm";
import Modal from "../components/Modal";
import QuickFilters from "../components/QuickFilters";
import { showToast } from "../components/toast";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";
import { getComponents, getDevelopers, getProjects } from "../api/create-bug-report-options";

/**
 * Element id of the "create bug report" modal, used to open it (via `data-bs-target`) and to close it from code.
 */
const createBugReportModalId = "create-bug-report-modal";

/**
 * Home page at `/`: the list of bug reports with quick filters and a "Create new" button that opens `CreateBugReportForm` in a modal.
 *
 * On mount it loads all reports (`GET /api/reports`) and the developers, projects and components that the create form offers as choices.
 * "Reported by me" and "Assigned to me" replace the list with `GET /api/reports/reported` or `/assigned`; Reset reloads all reports.
 * After a report is created the list is reloaded and the modal is closed. Failed requests show an error toast. Takes no props.
 */
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
                showToast("danger", "Failed to fetch bug reports!", "Unable to load reports");
            });
    }

    useEffect(() => {
        loadAllReports();
    }, []);

    useEffect(() => {
        getDevelopers()
            .done(setDevelopers)
            .fail(() => showToast("danger", "Failed to fetch developers.", "Unable to load developers"));

        getProjects()
            .done(setProjects)
            .fail(() => showToast("danger", "Failed to fetch projects.", "Unable to load projects"));

        getComponents()
            .done(setComponents)
            .fail(() => showToast("danger", "Failed to fetch components.", "Unable to load components"));
    }, []);

    function onReportedByMe() {
        setIsQuickFilterActive(true);

        getReported()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                showToast("danger", "Couldn't fetch your reported items!", "Unable to load reports");
            })
    }

    function onAssignedToMe() {
        setIsQuickFilterActive(true);

        getAssigned()
            .done((data) => {
                setBugReports(data);
            })
            .fail(() => {
                showToast("danger", "Couldn't fetch items assigned to you!", "Unable to load reports");
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
