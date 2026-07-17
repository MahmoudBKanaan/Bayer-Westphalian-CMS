import {
  clearAuthSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  saveAuthSession,
} from "@/auth/sessionStorageStrategy";

function resolveApiBaseUrl(
  configuredUrl: string | undefined,
  appEnvironment: string | undefined,
  productionBuild: boolean,
): string {
  const value = configuredUrl?.trim() || "http://localhost:8080/api";
  const isProduction = productionBuild || appEnvironment?.toLowerCase() === "prod";

  if (
    isProduction &&
    (value.includes("localhost") || value.includes("127.0.0.1") || value.startsWith("http://"))
  ) {
    throw new Error(
      "Production frontend API URL must be same-origin or use an explicit HTTPS origin.",
    );
  }

  return value.length > 1 ? value.replace(/\/$/, "") : value;
}

// Playwright imports shared frontend helpers in Node during test discovery, where Vite does not
// populate import.meta.env. The browser build still receives Vite's normal environment object.
const viteEnvironment = import.meta.env as ImportMetaEnv | undefined;
const API_BASE_URL = resolveApiBaseUrl(
  viteEnvironment?.VITE_API_BASE_URL,
  viteEnvironment?.VITE_APP_ENV,
  viteEnvironment?.PROD ?? false,
);

type ApiRequestInit = RequestInit & {
  authenticated?: boolean;
};

type RefreshResponse = {
  success: boolean;
  message: string;
  data: Parameters<typeof saveAuthSession>[0];
};

let refreshPromise: Promise<string | null> | null = null;

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
  const response = await sendApiRequest(path, requestInit, accessToken);

  if (response.status === 401 && authenticated) {
    const refreshedAccessToken = await refreshAccessToken();
    if (refreshedAccessToken != null) {
      return readApiResponse<T>(await sendApiRequest(path, requestInit, refreshedAccessToken));
    }
  }

  return readApiResponse<T>(response);
}

/** Binary file payload for report downloads (CSV/PDF attachments). */
export type DownloadedFile = {
  filename: string;
  contentType: string;
  blob: Blob;
};

/**
 * Authenticated binary download helper for report attachments (KB FR-109 / FR-110).
 *
 * Parses {@code Content-Disposition} for the filename when present.
 */
export async function apiDownload(path: string, init?: ApiRequestInit): Promise<DownloadedFile> {
  const { authenticated = true, ...requestInit } = init ?? {};
  const accessToken = authenticated ? getStoredAccessToken() : null;
  let response = await sendApiRequest(path, requestInit, accessToken);

  if (response.status === 401 && authenticated) {
    const refreshedAccessToken = await refreshAccessToken();
    if (refreshedAccessToken != null) {
      response = await sendApiRequest(path, requestInit, refreshedAccessToken);
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }

  const contentType = response.headers.get("Content-Type") ?? "application/octet-stream";
  const filename =
    parseContentDispositionFilename(response.headers.get("Content-Disposition")) ??
    fallbackFilename(path, contentType);
  const blob = await response.blob();
  return { filename, contentType, blob };
}

/**
 * Triggers a browser file download for a previously fetched attachment.
 */
export function triggerBrowserDownload(file: DownloadedFile): void {
  const objectUrl = URL.createObjectURL(file.blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = file.filename;
  anchor.rel = "noopener";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}

/** Parses RFC 5987 / simple filename= from Content-Disposition. */
export function parseContentDispositionFilename(
  header: string | null | undefined,
): string | null {
  if (header == null || header.trim() === "") {
    return null;
  }
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim().replace(/^"|"$/g, ""));
    } catch {
      return utf8Match[1].trim().replace(/^"|"$/g, "");
    }
  }
  const plainMatch = /filename="?([^";]+)"?/i.exec(header);
  if (plainMatch?.[1]) {
    return plainMatch[1].trim();
  }
  return null;
}

function fallbackFilename(path: string, contentType: string): string {
  const segment = path.split("/").filter(Boolean).at(-1) ?? "download";
  if (contentType.includes("csv")) {
    return segment.endsWith(".csv") ? segment : `${segment}.csv`;
  }
  if (contentType.includes("pdf")) {
    return segment.endsWith(".pdf") ? segment : `${segment}.pdf`;
  }
  return segment;
}

async function sendApiRequest(
  path: string,
  requestInit: RequestInit,
  accessToken: string | null,
) {
  const headers: Record<string, string> = {
    ...(isFormDataBody(requestInit.body) ? {} : { "Content-Type": "application/json" }),
    ...(accessToken == null ? {} : { Authorization: `Bearer ${accessToken}` }),
    ...headersToRecord(requestInit.headers),
  };

  return fetch(`${API_BASE_URL}${path}`, {
    ...requestInit,
    headers,
  });
}

async function readApiResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }

  return response.json() as Promise<T>;
}

async function refreshAccessToken() {
  const refreshToken = getStoredRefreshToken();
  if (refreshToken == null) {
    return null;
  }

  refreshPromise ??= requestTokenRefresh(refreshToken).finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
}

async function requestTokenRefresh(refreshToken: string) {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    clearAuthSession();
    return null;
  }

  const refreshResponse = (await response.json()) as RefreshResponse;
  saveAuthSession(refreshResponse.data);
  return refreshResponse.data.tokens.accessToken;
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

export { API_BASE_URL, resolveApiBaseUrl };
