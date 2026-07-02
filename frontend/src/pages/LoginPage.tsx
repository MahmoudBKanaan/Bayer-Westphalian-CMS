import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState } = useForm<LoginForm>({
    defaultValues: {
      email: "admin@bayer-westphalian.internal",
      password: "",
    },
  });

  function onSubmit(values: LoginForm) {
    const parsed = loginSchema.safeParse(values);
    if (!parsed.success) {
      return;
    }
    void navigate("/dashboard");
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <span className="eyebrow">Internal access</span>
        <h1>Bayer-Westphalian Campaign Management</h1>
        <form onSubmit={handleSubmit(onSubmit)} className="form-grid">
          <label>
            Email
            <input type="email" {...register("email", { required: true })} />
          </label>
          <label>
            Password
            <input type="password" {...register("password", { required: true })} />
          </label>
          <button type="submit">Sign in</button>
          {formState.isSubmitted && !formState.isValid ? (
            <p className="form-error">Enter a valid internal email and password.</p>
          ) : null}
        </form>
      </section>
    </main>
  );
}
