import { getStoredAccessToken } from "@/auth/sessionStorageStrategy";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

type ApiRequestInit = RequestInit & {
  authenticated?: boolean;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(path: string, init?: ApiRequestInit): Promise<T> {
  const { authenticated = true, ...requestInit } = init ?? {};
  const accessToken = authenticated ? getStoredAccessToken() : null;
  const headers: Record<string, string> = {
    ...(isFormDataBody(requestInit.body) ? {} : { "Content-Type": "application/json" }),
    ...(accessToken == null ? {} : { Authorization: `Bearer ${accessToken}` }),
    ...headersToRecord(requestInit.headers),
  };

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    headers,
  });

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }

  return response.json() as Promise<T>;
}

export function isAuthorizationError(error: unknown) {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

async function readErrorMessage(response: Response) {
  try {
    const body = (await response.json()) as unknown;
    if (isErrorBody(body)) {
      return body.message;
    }
  } catch {
    return `API request failed with ${response.status}`;
  }

  return `API request failed with ${response.status}`;
}

function isErrorBody(value: unknown): value is { message: string } {
  return (
    typeof value === "object" &&
    value != null &&
    "message" in value &&
    typeof value.message === "string" &&
    value.message.trim().length > 0
  );
}

function isFormDataBody(body: BodyInit | null | undefined) {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

function headersToRecord(headers: HeadersInit | undefined): Record<string, string> {
  if (headers == null) {
    return {};
  }

  if (headers instanceof Headers) {
    const normalizedHeaders: Record<string, string> = {};
    headers.forEach((value, key) => {
      normalizedHeaders[key] = value;
    });
    return normalizedHeaders;
  }

  if (Array.isArray(headers)) {
    return Object.fromEntries(headers);
  }

  return headers;
}

export { API_BASE_URL };
