import { useParams } from "react-router"

export default function BugReportPage() {
    const { id } = useParams();

    return (
        <div>
            Report ID: {id}
        </div>
    );
}