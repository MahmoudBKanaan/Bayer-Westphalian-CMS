import { apiRequest } from "@/api/client";

export type AuthenticatedUser = {
  id: string;
  email: string;
  fullName: string;
  status: "ACTIVE" | "DISABLED" | "LOCKED";
  lastLoginAt: string | null;
};

export type JwtTokenPair = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

export type AuthenticatedSession = {
  user: AuthenticatedUser;
  tokens: JwtTokenPair;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function login(email: string, password: string): Promise<AuthenticatedSession> {
  const response = await apiRequest<ApiResponse<AuthenticatedSession>>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
    authenticated: false,
  });

  return response.data;
}
