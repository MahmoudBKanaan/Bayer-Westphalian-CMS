import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";
import {
  getLoginNotice,
  getPostLoginPath,
  LOGIN_EMPLOYEE_HINT,
  LOGIN_PAGE_TITLE,
  LOGIN_PANEL_HEADING,
  loginErrorMessage,
  loginFormValidationMessages,
  loginSchema,
  type LoginFormValues,
  validateLoginForm,
} from "@/features/auth/loginFlow";
import {
  LOGIN_EMAIL_LABEL,
  LOGIN_FORM_ARIA_LABEL,
  LOGIN_PASSWORD_LABEL,
  LOGIN_SUBMIT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";

/**
 * Employee sign-in screen (KB FR-001 / item 598 — login flow works through UI).
 *
 * Validates credentials client-side, posts to AuthProvider.signIn, stores session,
 * and navigates to the dashboard or the originally requested protected path.
 */
export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signIn } = useAuth();
  const { register, handleSubmit, formState, setError, clearErrors } = useForm<LoginFormValues>({
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    clearErrors("root");
    const fieldErrors = validateLoginForm(values);
    if (Object.keys(fieldErrors).length > 0) {
      for (const [fieldName, message] of Object.entries(fieldErrors)) {
        if (fieldName === "email" || fieldName === "password") {
          setError(fieldName, { message, type: "manual" });
        }
      }
      return;
    }

    const parsed = loginSchema.safeParse(values);
    if (!parsed.success) {
      return;
    }

    try {
      await signIn(parsed.data.email, parsed.data.password);
    } catch (error) {
      setError("root", {
        message: loginErrorMessage(error),
        type: "server",
      });
      return;
    }

    void navigate(getPostLoginPath(location.state));
  }

  const authRequiredNotice = getLoginNotice(location.state);

  return (
    <main className="login-page" aria-labelledby="login-title">
      <section className="login-hero" aria-labelledby="login-title">
        <span className="brand-mark">BW</span>
        <div>
          <span className="eyebrow">Internal employee access</span>
          <h1 id="login-title">{LOGIN_PAGE_TITLE}</h1>
        </div>
      </section>
      <section className="login-panel" aria-labelledby="login-panel-heading">
        <div className="section-heading">
          <h2 id="login-panel-heading">{LOGIN_PANEL_HEADING}</h2>
          <span>{LOGIN_EMPLOYEE_HINT}</span>
        </div>
        {authRequiredNotice ? (
          <p className="form-error" role="alert" data-testid="login-auth-required-notice">
            {authRequiredNotice}
          </p>
        ) : null}
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="form-grid"
          noValidate
          aria-label={LOGIN_FORM_ARIA_LABEL}
        >
          <label>
            {LOGIN_EMAIL_LABEL}
            <input
              autoComplete="email"
              type="email"
              {...register("email", { required: loginFormValidationMessages.emailRequired })}
              aria-invalid={Boolean(formState.errors.email)}
            />
          </label>
          {formState.errors.email?.message ? (
            <p className="form-error">{formState.errors.email.message}</p>
          ) : null}
          <label>
            {LOGIN_PASSWORD_LABEL}
            <input
              autoComplete="current-password"
              type="password"
              {...register("password", {
                required: loginFormValidationMessages.passwordRequired,
              })}
              aria-invalid={Boolean(formState.errors.password)}
            />
          </label>
          {formState.errors.password?.message ? (
            <p className="form-error">{formState.errors.password.message}</p>
          ) : null}
          <button type="submit" disabled={formState.isSubmitting}>
            {formState.isSubmitting ? "Signing in..." : LOGIN_SUBMIT_LABEL}
          </button>
          {formState.errors.root?.message ? (
            <p className="form-error" role="alert" data-testid="login-error">
              {formState.errors.root.message}
            </p>
          ) : null}
        </form>
      </section>
    </main>
  );
}
