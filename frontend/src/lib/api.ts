import type {
  AdminOverviewResponse,
  AuthCredentialsRequest,
  AuthResponse,
  ApiSettings,
  AnalyticsSummaryResponse,
  CreateLinkRequest,
  LinkResponse,
  EmailVerificationConfirmRequest,
  EmailVerificationRequest,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  OAuthLoginCompleteRequest,
  RedirectPreviewResponse,
  UserProfileResponse,
} from '@/types';
import { loadSettings, normalizeBaseUrl, saveSettings } from '@/lib/settings';

export class ApiRequestError extends Error {
  status: number;
  code?: string;
  path?: string;

  constructor(message: string, status: number, code?: string, path?: string) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.path = path;
  }
}

let refreshSessionPromise: Promise<AuthResponse> | null = null;

function bearerAuthHeader(token: string) {
  if (!token) {
    return undefined;
  }
  return `Bearer ${token}`;
}

async function sendRequest(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
  includeCredentials = false,
) {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');

  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (includeAuth) {
    const authorization = bearerAuthHeader(settings.authToken);
    if (authorization) {
      headers.set('Authorization', authorization);
    }
  }

  return fetch(`${normalizeBaseUrl(settings.apiBaseUrl)}${path}`, {
    ...init,
    credentials: includeCredentials ? 'include' : 'same-origin',
    headers,
  });
}

async function parseError(response: Response) {
  const text = await response.text();
  try {
    const body = JSON.parse(text) as {
      message?: string;
      error?: string;
      code?: string;
      path?: string;
    };
    const message =
      body.message ?? body.error ?? body.code ?? `Request failed with ${response.status}`;
    return new ApiRequestError(message, response.status, body.code, body.path);
  } catch {
    return new ApiRequestError(text || `Request failed with ${response.status}`, response.status);
  }
}

async function refreshSession(settings: ApiSettings) {
  if (!refreshSessionPromise) {
    refreshSessionPromise = refreshTokens(settings).finally(() => {
      refreshSessionPromise = null;
    });
  }

  return refreshSessionPromise;
}

async function requestJson<T>(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
  includeCredentials = false,
  allowRefresh = true,
): Promise<T> {
  const response = await sendRequest(path, init, settings, includeAuth, includeCredentials);

  if (!response.ok) {
    if (
      response.status === 401 &&
      includeAuth &&
      allowRefresh &&
      path !== '/auth/refresh' &&
      path !== '/auth/logout'
    ) {
      try {
        const refreshed = await refreshSession(settings);
        settings.authToken = refreshed.token;
        saveSettings(settings);
        return requestJson<T>(path, init, settings, includeAuth, includeCredentials, false);
      } catch {
        settings.authToken = '';
        saveSettings(settings);
        throw await parseError(response);
      }
    }
    throw await parseError(response);
  }

  return response.json() as Promise<T>;
}

async function requestVoid(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
  includeCredentials = false,
) {
  const response = await sendRequest(path, init, settings, includeAuth, includeCredentials);
  if (!response.ok) {
    throw await parseError(response);
  }
}

export function buildApiBaseUrl(path: string, settings: ApiSettings = loadSettings()) {
  return `${normalizeBaseUrl(settings.apiBaseUrl)}${path}`;
}

export function createLink(request: CreateLinkRequest, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse>(
    '/urls',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
  );
}

export function register(request: AuthCredentialsRequest, settings: ApiSettings = loadSettings()) {
  return requestJson<AuthResponse>(
    '/auth/register',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function requestPasswordReset(
  request: PasswordResetRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestVoid(
    '/auth/password-reset/request',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function confirmPasswordReset(
  request: PasswordResetConfirmRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestVoid(
    '/auth/password-reset/confirm',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function requestEmailVerification(
  request: EmailVerificationRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestVoid(
    '/auth/email-verification/request',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function confirmEmailVerification(
  request: EmailVerificationConfirmRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestVoid(
    '/auth/email-verification/confirm',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function completeGithubLogin(
  request: OAuthLoginCompleteRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestJson<AuthResponse>(
    '/auth/oauth2/github/complete',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function login(request: AuthCredentialsRequest, settings: ApiSettings = loadSettings()) {
  return requestJson<AuthResponse>(
    '/auth/login',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    false,
    true,
  );
}

export function refreshTokens(settings: ApiSettings = loadSettings()) {
  return requestJson<AuthResponse>(
    '/auth/refresh',
    {
      method: 'POST',
    },
    settings,
    false,
    true,
    false,
  );
}

export function logoutSession(settings: ApiSettings = loadSettings()) {
  return requestVoid(
    '/auth/logout',
    {
      method: 'POST',
    },
    settings,
    false,
    true,
  );
}

export function getCurrentUser(settings: ApiSettings = loadSettings()) {
  return requestJson<UserProfileResponse>(
    '/auth/me',
    {
      method: 'GET',
    },
    settings,
  );
}

export function getAdminOverview(settings: ApiSettings = loadSettings()) {
  return requestJson<AdminOverviewResponse>(
    '/admin/overview',
    {
      method: 'GET',
    },
    settings,
  );
}

export function listLinks(limit: number = 10, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse[]>(
    `/urls?limit=${encodeURIComponent(String(limit))}`,
    { method: 'GET' },
    settings,
  );
}

export function getLink(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse>(
    `/urls/${encodeURIComponent(code)}`,
    { method: 'GET' },
    settings,
  );
}

export function getRedirectPreview(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<RedirectPreviewResponse>(
    `/urls/${encodeURIComponent(code)}/preview`,
    { method: 'GET' },
    settings,
  );
}

export function getAnalyticsSummary(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<AnalyticsSummaryResponse>(
    `/analytics/${encodeURIComponent(code)}`,
    { method: 'GET' },
    settings,
  );
}
