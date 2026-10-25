import { useTranslation } from "react-i18next";
import "./LanguageSwitcher.css";

/** Language toggle shared by the signed-in and signed-out navigation. */
export default function LanguageSwitcher() {
    const { t, i18n } = useTranslation();
    const isCzech = i18n.resolvedLanguage === "cs";
    const currentLanguage = t(isCzech ? "common.czech" : "common.english");
    const nextLanguage = t(isCzech ? "common.english" : "common.czech");

    return (
        <div className="d-flex align-items-center gap-2 flex-shrink-0">
            <i aria-hidden="true" className="bi bi-globe2 text-secondary" />
            <button
                type="button"
                className={`language-switcher${isCzech ? " language-switcher--cs" : ""}`}
                aria-label={`${t("common.language")}: ${currentLanguage}`}
                title={`${t("common.language")}: ${nextLanguage}`}
                onClick={() => { void i18n.changeLanguage(isCzech ? "en" : "cs"); }}
            >
                <span className="language-switcher__label" aria-hidden="true">
                    {isCzech ? "CS" : "EN"}
                </span>
                <span className="language-switcher__flag" aria-hidden="true">
                    <svg viewBox="0 0 60 30" preserveAspectRatio="none" focusable="false">
                        {isCzech ? (
                            <>
                                <path fill="#fff" d="M0 0h60v30H0z" />
                                <path fill="#d7141a" d="M0 15h60v15H0z" />
                                <path fill="#11457e" d="M0 0l30 15L0 30z" />
                            </>
                        ) : (
                            <>
                                <path fill="#012169" d="M0 0h60v30H0z" />
                                <path stroke="#fff" strokeWidth="6" d="M0 0l60 30M60 0L0 30" />
                                <path fill="#c8102e" d="M0 0l30 15h-5L0 2zM60 0L30 15v-3L55 0zM60 30L30 15h5l25 13zM0 30l30-15v3L5 30z" />
                                <path stroke="#fff" strokeWidth="10" d="M30 0v30M0 15h60" />
                                <path stroke="#c8102e" strokeWidth="6" d="M30 0v30M0 15h60" />
                            </>
                        )}
                    </svg>
                </span>
            </button>
        </div>
    );
}
