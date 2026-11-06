export interface AuthenticatedUser {
  id: number;
  username: string;
  email: string;
}

export interface LoginLocation {
  latitude: number;
  longitude: number;
  accuracyMeters: number;
}

export interface TwoFactorSetup {
  secret: string;
  provisioningUri: string;
}

export type SignInResult =
  | { requiresTwoFactor: true; challengeToken: string }
  | { requiresTwoFactor: false; user: AuthenticatedUser };

export class AuthApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "AuthApiError";
  }
}

const apiBaseUrl = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/$/, "");
let refreshInFlight: Promise<boolean> | null = null;

async function errorMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json();
    if (
      typeof body === "object" &&
      body !== null &&
      "message" in body &&
      typeof body.message === "string"
    ) {
      return body.message;
    }
  } catch {
    // Error responses may not include a JSON body.
  }
  return `Request failed (${response.status}).`;
}

async function getCsrfToken(): Promise<string> {
  const response = await fetch(`${apiBaseUrl}/api/auth/csrf`, {
    credentials: "include",
  });
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("token" in body) ||
    typeof body.token !== "string"
  ) {
    throw new Error(
      "The authentication service returned an invalid CSRF token.",
    );
  }
  return body.token;
}

export async function sessionFetch(
  path: string,
  init: RequestInit = {},
): Promise<Response> {
  const response = await performSessionFetch(path, init);
  if (
    response.status !== 401 ||
    path === "/api/auth/login" ||
    path === "/api/auth/refresh"
  ) {
    return response;
  }
  if (typeof navigator !== "undefined" && !navigator.onLine) {
    throw new AuthApiError("The authentication service is offline.", 503);
  }

  refreshInFlight ??= refreshSession().finally(() => {
    refreshInFlight = null;
  });
  if (!(await refreshInFlight)) return response;
  return performSessionFetch(path, init);
}

async function performSessionFetch(
  path: string,
  init: RequestInit,
): Promise<Response> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
    headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }

  return fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: "include",
    headers,
  });
}

async function refreshSession(): Promise<boolean> {
  if (typeof navigator !== "undefined" && !navigator.onLine) {
    throw new AuthApiError("The authentication service is offline.", 503);
  }
  const response = await performSessionFetch("/api/auth/refresh", {
    method: "POST",
  });
  if (response.status === 401) return false;
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
  return true;
}

export async function fetchCurrentUser(): Promise<AuthenticatedUser> {
  const response = await sessionFetch("/api/auth/me");
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
  return response.json() as Promise<AuthenticatedUser>;
}

export async function signIn(
  email: string,
  password: string,
  location: LoginLocation | null = null,
): Promise<SignInResult> {
  const response = await sessionFetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, location }),
  });
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
  const body: unknown = await response.json();
  if (
    typeof body === "object" &&
    body !== null &&
    "requiresTwoFactor" in body &&
    body.requiresTwoFactor === true &&
    "challengeToken" in body &&
    typeof body.challengeToken === "string"
  ) {
    return { requiresTwoFactor: true, challengeToken: body.challengeToken };
  }
  return { requiresTwoFactor: false, user: parseAuthenticatedUser(body) };
}

export async function verifyTwoFactorLogin(
  challengeToken: string,
  code: string,
  location: LoginLocation | null,
): Promise<AuthenticatedUser> {
  const response = await sessionFetch("/api/auth/verify-two-factor", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ challengeToken, code, location }),
  });
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
  return parseAuthenticatedUser(await response.json());
}

export async function fetchTwoFactorStatus(): Promise<boolean> {
  const response = await sessionFetch("/api/auth/totp");
  if (!response.ok) {
    throw await getResponseError(response);
  }
  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("enabled" in body) ||
    typeof body.enabled !== "boolean"
  ) {
    throw new Error(
      "The authentication service returned an invalid 2FA status.",
    );
  }
  return body.enabled;
}

export async function beginTwoFactorSetup(
  currentPassword: string,
): Promise<TwoFactorSetup> {
  const response = await sessionFetch("/api/auth/totp/setup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword }),
  });
  if (!response.ok) {
    throw await getResponseError(response);
  }
  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("secret" in body) ||
    typeof body.secret !== "string" ||
    !("provisioningUri" in body) ||
    typeof body.provisioningUri !== "string"
  ) {
    throw new Error(
      "The authentication service returned invalid 2FA setup data.",
    );
  }
  return { secret: body.secret, provisioningUri: body.provisioningUri };
}

export async function enableTwoFactor(code: string): Promise<void> {
  const response = await sessionFetch("/api/auth/totp/enable", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });
  if (!response.ok) {
    throw await getResponseError(response);
  }
}

export async function disableTwoFactor(
  currentPassword: string,
  code: string,
): Promise<void> {
  const response = await sessionFetch("/api/auth/totp", {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword, code }),
  });
  if (!response.ok) {
    throw await getResponseError(response);
  }
}

export async function signOut(): Promise<void> {
  const response = await sessionFetch("/api/auth/logout", { method: "POST" });
  if (!response.ok) {
    throw new AuthApiError(await errorMessage(response), response.status);
  }
}

export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  const response = await sessionFetch("/api/auth/change-password", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
  if (!response.ok) {
    throw await getResponseError(response);
  }
}

export async function getResponseError(
  response: Response,
): Promise<AuthApiError> {
  return new AuthApiError(await errorMessage(response), response.status);
}

function parseAuthenticatedUser(body: unknown): AuthenticatedUser {
  if (
    typeof body === "object" &&
    body !== null &&
    "id" in body &&
    typeof body.id === "number" &&
    "username" in body &&
    typeof body.username === "string" &&
    "email" in body &&
    typeof body.email === "string"
  ) {
    return { id: body.id, username: body.username, email: body.email };
  }
  throw new Error("The authentication service returned invalid user data.");
}
