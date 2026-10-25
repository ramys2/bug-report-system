import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router";
import { registerAccount } from "../api/account";
import { getApiErrorMessage } from "../api/error";
import { showToast } from "./toast";

/** Registers a Reporter through the existing accounts API, then redirects to login without signing in. */
export default function RegistrationForm() {
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const [formValues, setFormValues] = useState({ name: "", email: "", password: "", repeatPassword: "" });
    const [showPassword, setShowPassword] = useState(false);
    const [showRepeatPassword, setShowRepeatPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormValues((currentValues) => ({ ...currentValues, [name]: value }));
    }

    function handleSubmit(event) {
        event.preventDefault();
        if (isSubmitting) {
            return;
        }

        if (!formValues.name.trim() || !formValues.email.trim() || !formValues.password.trim() || !formValues.repeatPassword.trim()) {
            showToast("warning", t("common.requiredFields"), t("common.missingInformation"));
            return;
        }

        if (formValues.password !== formValues.repeatPassword) {
            showToast("warning", t("auth.passwordsDoNotMatch"), t("common.missingInformation"));
            return;
        }

        // BCrypt's limit is in UTF-8 bytes, so accented characters can use more than one byte.
        if (new TextEncoder().encode(formValues.password).length > 72) {
            showToast("warning", t("auth.passwordTooLong"), t("common.missingInformation"));
            return;
        }

        setIsSubmitting(true);
        registerAccount({ name: formValues.name.trim(), email: formValues.email.trim(), password: formValues.password })
            .done(() => {
                showToast("success", i18n.t("auth.registrationSuccess"));
                void navigate("/login", { replace: true });
            })
            .fail((xhr) => showToast("danger", getApiErrorMessage(xhr), i18n.t("auth.registrationFailed")))
            .always(() => setIsSubmitting(false));
    }

    return (
        <form className="border rounded-4 p-4" onSubmit={handleSubmit}>
            <h1 className="h4 mb-4">{t("auth.register")}</h1>
            <div className="mb-3">
                <label className="form-label" htmlFor="registration-name">{t("auth.username")}</label>
                <input
                    autoComplete="username"
                    className="form-control"
                    disabled={isSubmitting}
                    id="registration-name"
                    maxLength={255}
                    name="name"
                    onChange={handleChange}
                    required
                    value={formValues.name}
                />
            </div>
            <div className="mb-3">
                <label className="form-label" htmlFor="registration-email">{t("auth.email")}</label>
                <input
                    autoComplete="email"
                    className="form-control"
                    disabled={isSubmitting}
                    id="registration-email"
                    maxLength={255}
                    name="email"
                    onChange={handleChange}
                    required
                    type="email"
                    value={formValues.email}
                />
            </div>
            <div className="mb-3">
                <label className="form-label" htmlFor="registration-password">{t("auth.password")}</label>
                <div className="input-group">
                    <input
                        aria-describedby="registration-password-hint"
                        autoComplete="new-password"
                        className="form-control"
                        disabled={isSubmitting}
                        id="registration-password"
                        name="password"
                        onChange={handleChange}
                        required
                        type={showPassword ? "text" : "password"}
                        value={formValues.password}
                    />
                    <button
                        aria-controls="registration-password"
                        aria-label={t(showPassword ? "auth.hidePassword" : "auth.showPassword")}
                        aria-pressed={showPassword}
                        className="btn btn-outline-secondary"
                        disabled={isSubmitting}
                        onClick={() => setShowPassword((current) => !current)}
                        type="button"
                    >
                        <i aria-hidden="true" className={showPassword ? "bi bi-eye-slash" : "bi bi-eye"} />
                    </button>
                </div>
                <div className="form-text" id="registration-password-hint">{t("auth.passwordHint")}</div>
            </div>
            <div className="mb-4">
                <label className="form-label" htmlFor="registration-repeat-password">{t("auth.repeatPassword")}</label>
                <div className="input-group">
                    <input
                        autoComplete="new-password"
                        className="form-control"
                        disabled={isSubmitting}
                        id="registration-repeat-password"
                        name="repeatPassword"
                        onChange={handleChange}
                        required
                        type={showRepeatPassword ? "text" : "password"}
                        value={formValues.repeatPassword}
                    />
                    <button
                        aria-controls="registration-repeat-password"
                        aria-label={t(showRepeatPassword ? "auth.hideRepeatPassword" : "auth.showRepeatPassword")}
                        aria-pressed={showRepeatPassword}
                        className="btn btn-outline-secondary"
                        disabled={isSubmitting}
                        onClick={() => setShowRepeatPassword((current) => !current)}
                        type="button"
                    >
                        <i aria-hidden="true" className={showRepeatPassword ? "bi bi-eye-slash" : "bi bi-eye"} />
                    </button>
                </div>
            </div>
            <button className="btn btn-primary w-100" disabled={isSubmitting} type="submit">
                {isSubmitting ? t("common.creating") : t("auth.register")}
            </button>
            <div className="border-top my-3" />
            <p className="mb-2">{t("auth.haveAccount")}</p>
            <Link className="btn btn-outline-primary w-100" to="/login">{t("auth.signIn")}</Link>
        </form>
    );
}
