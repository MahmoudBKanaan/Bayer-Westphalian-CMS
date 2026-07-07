import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { isAuthorizationError } from "@/api/client";
import { useAuth } from "@/auth/AuthProvider";

const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid internal email address."),
  password: z.string().min(8, "Password must be at least 8 characters."),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signIn } = useAuth();
  const { register, handleSubmit, formState, setError, clearErrors } = useForm<LoginForm>({
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginForm) {
    clearErrors("root");
    const parsed = loginSchema.safeParse(values);
    if (!parsed.success) {
      for (const issue of parsed.error.issues) {
        const fieldName = issue.path[0];
        if (fieldName === "email" || fieldName === "password") {
          setError(fieldName, { message: issue.message, type: "manual" });
        }
      }
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

  return (
    <main className="login-page">
      <section className="login-hero" aria-labelledby="login-title">
        <span className="brand-mark">BW</span>
        <div>
          <span className="eyebrow">Internal employee access</span>
          <h1 id="login-title">Bayer-Westphalian Campaign Management</h1>
        </div>
      </section>
      <section className="login-panel">
        <div className="section-heading">
          <h2>Sign in</h2>
          <span>Use your employee account</span>
        </div>
        {getLoginNotice(location.state) ? (
          <p className="form-error" role="alert">
            {getLoginNotice(location.state)}
          </p>
        ) : null}
        <form onSubmit={handleSubmit(onSubmit)} className="form-grid" noValidate>
          <label>
            Email
            <input
              autoComplete="email"
              type="email"
              {...register("email", { required: "Email is required." })}
              aria-invalid={Boolean(formState.errors.email)}
            />
          </label>
          {formState.errors.email?.message ? (
            <p className="form-error">{formState.errors.email.message}</p>
          ) : null}
          <label>
            Password
            <input
              autoComplete="current-password"
              type="password"
              {...register("password", { required: "Password is required." })}
              aria-invalid={Boolean(formState.errors.password)}
            />
          </label>
          {formState.errors.password?.message ? (
            <p className="form-error">{formState.errors.password.message}</p>
          ) : null}
          <button type="submit" disabled={formState.isSubmitting}>
            {formState.isSubmitting ? "Signing in..." : "Sign in"}
          </button>
          {formState.errors.root?.message ? (
            <p className="form-error" role="alert">
              {formState.errors.root.message}
            </p>
          ) : null}
        </form>
      </section>
    </main>
  );
}

function loginErrorMessage(error: unknown) {
  if (isAuthorizationError(error)) {
    return "Login failed. Check your credentials or account status.";
  }

  return "Login failed. Try again or contact an administrator.";
}

function getLoginNotice(state: unknown) {
  if (
    typeof state === "object" &&
    state != null &&
    "reason" in state &&
    state.reason === "auth-required"
  ) {
    return "Sign in with an authorized employee account to continue.";
  }

  return "";
}

function getPostLoginPath(state: unknown) {
  if (
    typeof state === "object" &&
    state != null &&
    "from" in state &&
    typeof state.from === "object" &&
    state.from != null &&
    "pathname" in state.from &&
    typeof state.from.pathname === "string"
  ) {
    return state.from.pathname;
  }

  return "/dashboard";
}
