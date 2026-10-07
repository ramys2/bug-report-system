import { useEffect, useState } from "react";
import BugReportList from "../components/BugReportList";
import CreateBugReportForm from "../components/CreateBugReportForm";
import Modal from "../components/Modal";
import QuickFilters from "../components/QuickFilters";
import { showToast } from "../components/toast";
import "./HomePage.css";
import { getAllReports, getAssigned, getReported } from "../api/bug-report";
import { getDevelopers } from "../api/account";
import { getAllProjects } from "../api/project";

/**
 * Home page at `/`: the list of bug reports with quick filters and a "Create new" button that opens `CreateBugReportForm` in a modal.
 *
 * On mount it loads all reports (`GET /api/reports`). The developers and projects that the create form offers as choices are loaded when "Create new" is clicked (the form loads the components itself).
 * "Reported by me" and "Assigned to me" replace the list with `GET /api/reports/reported` or `/assigned`; Reset reloads all reports.
 * After a report is created the list is reloaded and the modal is closed. Failed requests show an error toast. Takes no props.
 */
function HomePage() {
    const [bugReports, setBugReports] = useState([]);
    const [isQuickFilterActive, setIsQuickFilterActive] = useState(false);
    const [developers, setDevelopers] = useState([]);
    const [projects, setProjects] = useState([]);
    const [isCreateOpen, setIsCreateOpen] = useState(false);

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

    function loadCreateFormOptions() {
        // Only fetch what is still missing, so reopening the modal doesn't repeat the requests (a failed request is retried on the next click).
        if (developers.length === 0) {
            getDevelopers()
                .done(setDevelopers)
                .fail(() => showToast("danger", "Failed to fetch developers.", "Unable to load developers"));
        }

        if (projects.length === 0) {
            getAllProjects()
                .done(setProjects)
                .fail(() => showToast("danger", "Failed to fetch projects.", "Unable to load projects"));
        }
    }

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
        setIsCreateOpen(false);
    }

    function openCreateForm() {
        loadCreateFormOptions();
        setIsCreateOpen(true);
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
                        onClick={openCreateForm}
                    >
                        Create new +
                    </button>
                </div>
                <BugReportList reports={bugReports} />
                <Modal onHide={() => setIsCreateOpen(false)} show={isCreateOpen}>
                    <CreateBugReportForm
                        developers={developers}
                        projects={projects}
                        onCreated={onReportCreated}
                    />
                </Modal>
            </main>
        </div>
    );
}

export default HomePage;
