import { Link } from "react-router";
import { useTranslation } from "react-i18next";

/**
 * Page shown for any path that no other route matches (the `*` route in `App.jsx`). Offers a link back to `/`. Takes no props.
 */
function NotFoundPage() {
    const { t } = useTranslation();

    return (
        <main className="container d-flex flex-column flex-grow-1 align-items-center justify-content-center py-5 text-center">
            <h1 className="display-4">{t("errors.pageNotFound")}</h1>
            <p className="text-muted">{t("errors.pageNotFoundDescription")}</p>
            <Link to="/" className="btn btn-primary">
                {t("common.backToHome")}
            </Link>
        </main>
    );
}

export default NotFoundPage;
