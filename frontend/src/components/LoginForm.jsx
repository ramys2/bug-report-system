import { useContext, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { getAuthenticationMethods, login } from "../api/auth";
import { getApiErrorMessage } from "../api/error";
import "./LoginForm.css";
import { Link, useLocation, useNavigate } from "react-router";
import AuthContext from "./AuthContext";
import { showToast } from "./toast";

/**
 * Email and password form.
 *
 * On submit it calls `login()` (`POST /api/auth/login`); on success it reloads the current user through `AuthContext` and navigates back to the
 * page the user originally requested (`location.state.from`) or to `/`. A failed login shows a localized error toast resolved from the backend code or a safe fallback.
 * The Register link opens the registration page. Takes no props.
 */
function LoginForm() {
    const { t } = useTranslation();
    const auth = useContext(AuthContext);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const [googleLoginPath, setGoogleLoginPath] = useState(null);
    const consumedError = useRef(null);
    const from = location.state?.from;
    const returnTo = from?.pathname ? `${from.pathname}${from.search ?? ""}${from.hash ?? ""}` : "/";
    const googleLoginUrl = `${googleLoginPath}?${new URLSearchParams({ returnTo })}`;

    useEffect(() => {
        const request = getAuthenticationMethods()
            .done((methods) => {
                if (methods.googleEnabled === true && typeof methods.googleLoginUrl === "string"
                        && /^\/api\/(?:[A-Za-z0-9_-]+\/)*google$/.test(methods.googleLoginUrl)) {
                    setGoogleLoginPath(methods.googleLoginUrl);
                }
            });
        return () => request.abort();
    }, []);

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const error = params.get("oauthError");
        if (error === null || consumedError.current === location.search) return;
        consumedError.current = location.search;
        const code = ["GOOGLE_EMAIL_CONFLICT", "GOOGLE_EMAIL_NOT_VERIFIED"].includes(error)
            ? error : "GOOGLE_LOGIN_FAILED";
        showToast("danger", getApiErrorMessage({ responseJSON: { code } }), t("auth.loginFailed"));
        params.delete("oauthError");
        void navigate({ pathname: location.pathname, search: params.toString(), hash: location.hash },
            { replace: true, state: location.state });
    }, [location, navigate, t]);

    function handleSubmit(event) {
        event.preventDefault();

        login(email, password)
            .done(() => {
                auth.loadCurrentUser()
                    .done(() => navigate(location.state?.from ?? "/", { replace: true }));
            })
            .fail((xhr) => {
                showToast("danger", getApiErrorMessage(xhr), t("auth.loginFailed"));
            });
    }

    return (
        <form className="border rounded-4 p-4" onSubmit={handleSubmit}>
            <div className="mb-3">
                <label className="visually-hidden" htmlFor="email">
                    {t("auth.email")}
                </label>
                <input
                    type="email"
                    id="email"
                    className="form-control"
                    placeholder={t("auth.email")}
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                />
            </div>

            <div className="mb-4">
                <label className="visually-hidden" htmlFor="password">
                    {t("auth.password")}
                </label>
                <div className="input-group">
                    <input
                        type={showPassword ? "text" : "password"}
                        id="password"
                        className="form-control"
                        placeholder={t("auth.password")}
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                    />
                    <button
                        aria-controls="password"
                        aria-label={t(showPassword ? "auth.hidePassword" : "auth.showPassword")}
                        aria-pressed={showPassword}
                        className="btn btn-outline-secondary"
                        onClick={() => setShowPassword((current) => !current)}
                        type="button"
                    >
                        <i aria-hidden="true" className={showPassword ? "bi bi-eye-slash" : "bi bi-eye"} />
                    </button>
                </div>
            </div>

            <button type="submit" className="btn btn-primary w-100">
                {t("auth.signIn")}
            </button>

            {googleLoginPath && (
                <a className="btn btn-outline-primary w-100 mt-3" href={googleLoginUrl}>
                    {t("auth.signInWithGoogle")}
                </a>
            )}

            <div className="border-top my-3" />

            <p className="mb-2">{t("auth.noAccount")}</p>
            <Link to="/register" className="btn btn-outline-primary w-100">
                {t("auth.register")}
            </Link>
        </form>
    );
}

export default LoginForm;
