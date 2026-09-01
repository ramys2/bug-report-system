import { useParams } from "react-router"
import Navbar from "../components/Navbar";
import { useEffect, useState } from "react";
import { getReport } from "../api/bug-report";
import { getComments } from "../api/comment";

export default function BugReportPage() {
    const { id } = useParams();
    const [bugReport, setBugReport] = useState({});
    const [comments, setComments] = useState([]);

    useEffect(() => {
        getReport(id)
            .done((data) => {
                setBugReport(data);

                getComments(id)
                    .done((comments) => {
                        setComments(comments);
                    })
                    .fail(() => {
                        alert ("Unable to fetch comments!");
                    })
            })
            .fail(() => {
                alert("Unable to fetch bug report!");
            });
    }, []);

    return (
        <div>
            <Navbar />
        </div>
    );
}