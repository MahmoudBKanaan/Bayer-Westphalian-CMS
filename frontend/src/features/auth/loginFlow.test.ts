import { describe, expect, it } from "vitest";
import { ApiError } from "@/api/client";
import {
  formatLoginFlowJourney,
  getLoginNotice,
  getPostLoginPath,
  isValidLoginFlowOrder,
  LOGIN_AUTH_REQUIRED_NOTICE,
  LOGIN_DEFAULT_LANDING_PATH,
  LOGIN_GENERIC_FAILURE_MESSAGE,
  LOGIN_INVALID_CREDENTIALS_MESSAGE,
  LOGIN_RATE_LIMITED_MESSAGE,
  loginErrorMessage,
  loginFlowStepIdsInOrder,
  loginFormValidationMessages,
  validateLoginForm,
} from "@/features/auth/loginFlow";

describe("loginFlow (item 598)", () => {
  it("documents the UI login journey open → credentials → submit → protected landing", () => {
    expect(loginFlowStepIdsInOrder()).toEqual([
      "open-login",
      "enter-credentials",
      "submit",
      "land-protected",
    ]);
    expect(formatLoginFlowJourney()).toBe(
      "Open sign-in → Enter credentials → Submit sign-in → Land on protected UI",
    );
    expect(isValidLoginFlowOrder(loginFlowStepIdsInOrder())).toBe(true);
    expect(isValidLoginFlowOrder(["submit", "open-login"] as never)).toBe(false);
  });

  it("defaults post-login navigation to the dashboard", () => {
    expect(getPostLoginPath(undefined)).toBe(LOGIN_DEFAULT_LANDING_PATH);
    expect(getPostLoginPath(null)).toBe(LOGIN_DEFAULT_LANDING_PATH);
    expect(getPostLoginPath({})).toBe(LOGIN_DEFAULT_LANDING_PATH);
    expect(getPostLoginPath({ from: { pathname: "" } })).toBe(LOGIN_DEFAULT_LANDING_PATH);
  });

  it("returns employees to the protected path they originally requested", () => {
    expect(
      getPostLoginPath({
        from: { pathname: "/campaigns" },
        reason: "auth-required",
      }),
    ).toBe("/campaigns");
    expect(getPostLoginPath({ from: { pathname: "/users" } })).toBe("/users");
  });

  it("rejects open-redirect style return paths", () => {
    expect(getPostLoginPath({ from: { pathname: "//evil.example" } })).toBe(
      LOGIN_DEFAULT_LANDING_PATH,
    );
    expect(getPostLoginPath({ from: { pathname: "https://evil.example" } })).toBe(
      LOGIN_DEFAULT_LANDING_PATH,
    );
  });

  it("shows an auth-required notice only when ProtectedRoute redirected", () => {
    expect(getLoginNotice({ reason: "auth-required" })).toBe(LOGIN_AUTH_REQUIRED_NOTICE);
    expect(getLoginNotice({})).toBe("");
    expect(getLoginNotice(undefined)).toBe("");
  });

  it("maps login API failures to safe UI messages", () => {
    expect(loginErrorMessage(new ApiError(401, "Invalid email or password"))).toBe(
      LOGIN_INVALID_CREDENTIALS_MESSAGE,
    );
    expect(loginErrorMessage(new ApiError(403, "Forbidden"))).toBe(
      LOGIN_INVALID_CREDENTIALS_MESSAGE,
    );
    expect(loginErrorMessage(new ApiError(429, "LOGIN_RATE_LIMITED"))).toBe(
      LOGIN_RATE_LIMITED_MESSAGE,
    );
    expect(loginErrorMessage(new ApiError(500, "boom"))).toBe(LOGIN_GENERIC_FAILURE_MESSAGE);
    expect(loginErrorMessage(new Error("network"))).toBe(LOGIN_GENERIC_FAILURE_MESSAGE);
  });

  it("validates email and password before the backend is called", () => {
    expect(validateLoginForm({ email: "not-an-email", password: "short" })).toEqual({
      email: loginFormValidationMessages.emailInvalid,
      password: loginFormValidationMessages.passwordMinLength,
    });
    expect(
      validateLoginForm({
        email: "admin@bayer-westphalian.test",
        password: "StrongPassword!2026",
      }),
    ).toEqual({});
  });
});
