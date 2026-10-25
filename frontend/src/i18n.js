import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "./locales/en.json";
import cs from "./locales/cs.json";

const languageStorageKey = "language";
const supportedLanguages = ["en", "cs"];

function getStoredLanguage() {
    try {
        const language = localStorage.getItem(languageStorageKey);
        return supportedLanguages.includes(language) ? language : "en";
    } catch {
        // Browsers can block storage; the UI should still work in English.
        return "en";
    }
}

i18n.on("languageChanged", (language) => {
    document.documentElement.lang = language;

    try {
        localStorage.setItem(languageStorageKey, language);
    } catch {
        // Changing language still works when preferences cannot be saved.
    }
});

void i18n.use(initReactI18next).init({
    resources: {
        en: { translation: en },
        cs: { translation: cs }
    },
    lng: getStoredLanguage(),
    fallbackLng: "en",
    supportedLngs: supportedLanguages,
    // Translations are bundled locally, so initialize before React renders.
    initAsync: false,
    interpolation: {
        // React escapes text rendered in JSX.
        escapeValue: false
    }
});

export default i18n;
