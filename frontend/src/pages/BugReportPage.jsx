import { useEffect, useState } from "react";
import { useParams } from "react-router";
import { getCurrentUser } from "../api/auth";
import { getReport } from "../api/bug-report";
import { getComments } from "../api/comment";
import Navbar from "../components/Navbar";
import "./BugReportPage.css";

function displayValue(value) {
    return value || "Not provided";
}

function formatDate(value) {
    if (!value) {
        return "Not provided";
    }

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString();
}

function ReportDetail({ label, value }) {
    return (
        <div className="col-12 col-sm-6">
            <dt className="small text-secondary fw-semibold">{label}</dt>
            <dd className="mb-0">{displayValue(value)}</dd>
        </div>
    );
}

function ReportSection({ title, children }) {
    return (
        <section className="border rounded-4 p-3 h-100">
            <h2 className="h5 mb-3">{title}</h2>
            <div className="mb-0 text-break text-pre-wrap">{displayValue(children)}</div>
        </section>
    );
}

export default function BugReportPage() {
    const { id } = useParams();
    const [bugReport, setBugReport] = useState(null);
    const [comments, setComments] = useState([]);
    const [currentUser, setCurrentUser] = useState(null);

    useEffect(() => {
        getReport(id)
            .done((data) => {
                setBugReport(data);
            })
            .fail(() => {
                alert("Unable to fetch bug report!");
            });

        getComments(id)
            .done((data) => {
                setComments(data);
            })
            .fail(() => {
                alert("Unable to fetch comments!");
            });

        getCurrentUser()
            .done((user) => {
                setCurrentUser(user);
            });
    }, [id]);

    return (
        <div className="bug-report-page d-flex flex-column">
            <Navbar />
            <main className="container flex-grow-1 py-4 text-start">
                {bugReport === null ? (
                    <p className="text-secondary mb-0">Loading bug report...</p>
                ) : (
                    <>
                        <div className="row g-4 mb-4">
                            <div className="col-12 col-lg-5">
                                <section className="border rounded-4 p-3 h-100">
                                    <h1 className="h3 mb-1">{displayValue(bugReport.title)}</h1>
                                    <p className="text-secondary small mb-4">ID: {bugReport.id}</p>
                                    <dl className="row g-3 mb-0">
                                        <ReportDetail label="Reporter" value={bugReport.reporterName} />
                                        <ReportDetail label="Assignee" value={bugReport.assigneeName} />
                                        <ReportDetail label="Severity" value={bugReport.severity} />
                                        <ReportDetail label="Status" value={bugReport.status} />
                                        <ReportDetail label="Project" value={bugReport.projectName} />
                                        <ReportDetail label="Component" value={bugReport.componentName} />
                                        <ReportDetail label="Created at" value={formatDate(bugReport.createdAt)} />
                                        <ReportDetail label="Updated at" value={formatDate(bugReport.updatedAt)} />
                                    </dl>
                                </section>
                            </div>
                            <div className="col-12 col-lg-7">
                                <ReportSection title="Description">
                                    {bugReport.description}
                                </ReportSection>
                            </div>
                        </div>

                        <div className="row g-3 mb-3">
                            <div className="col-12">
                                <ReportSection title="Steps to reproduce">
                                    {bugReport.stepsToReproduce}
                                </ReportSection>
                            </div>
                            <div className="col-12 col-lg-6">
                                <ReportSection title="Expected behavior">
                                    {bugReport.expectedBehavior}
                                </ReportSection>
                            </div>
                            <div className="col-12 col-lg-6">
                                <ReportSection title="Actual behavior">
                                    {bugReport.actualBehavior}
                                </ReportSection>
                            </div>
                        </div>

                        <section className="border-top pt-4 mt-4">
                            <h2 className="h4 mb-3">Comments</h2>
                            {comments.length === 0 ? (
                                <p className="text-secondary mb-0">No comments yet.</p>
                            ) : (
                                <div className="d-flex flex-column gap-3">
                                    {comments.map((comment) => (
                                        <CommentCard
                                            comment={comment}
                                            isCurrentUser={comment.authorId === currentUser?.id}
                                            key={comment.id}
                                        />
                                    ))}
                                </div>
                            )}
                        </section>
                    </>
                )}
            </main>
        </div>
    );
}

function CommentCard({ comment, isCurrentUser }) {
    return (
        <div className="row">
            <div className={`col-12 col-md-6 ${isCurrentUser ? "ms-md-auto" : ""}`}>
                <article className="border rounded-4 p-3">
                    <div className="d-flex justify-content-between gap-3 mb-2">
                        <h3 className="h6 mb-0">
                            {displayValue(comment.authorName)}
                        </h3>
                        <time className="small text-secondary text-nowrap" dateTime={comment.createdAt}>
                            {formatDate(comment.createdAt)}
                        </time>
                    </div>
                    <p className="mb-0 text-break text-pre-wrap">{comment.content}</p>
                </article>
            </div>
        </div>
    );
}
