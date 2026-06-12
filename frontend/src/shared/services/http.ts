import type {
  AccountProfileResponse,
  AdminOverviewResponse,
  AnalyticsSummaryResponse,
  ApiSettings,
  AuthCredentialsRequest,
  AuthResponse,
  CreateLinkRequest,
  EmailVerificationConfirmRequest,
  EmailVerificationRequest,
  LinkResponse,
  OAuthLoginCompleteRequest,
  PasswordChangeRequest,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  RedirectPreviewResponse,
  UserProfileResponse,
} from '@/shared/types/api';
import { loadSettings, normalizeBaseUrl, saveSettings } from '@/shared/services/settings';

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
    refreshSessionPromise = requestAuthJson<AuthResponse>(
      '/auth/refresh',
      { method: 'POST' },
      settings,
      false,
      true,
    ).finally(() => {
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
  allowRefresh = true,
) {
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
        await requestVoid(path, init, settings, includeAuth, includeCredentials, false);
        return;
      } catch {
        settings.authToken = '';
        saveSettings(settings);
        throw await parseError(response);
      }
    }
    throw await parseError(response);
  }
}

export function buildApiBaseUrl(path: string, settings: ApiSettings = loadSettings()) {
  return `${normalizeBaseUrl(settings.apiBaseUrl)}${path}`;
}

export function requestAuthJson<T>(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
  includeCredentials = false,
  allowRefresh = true,
) {
  return requestJson<T>(path, init, settings, includeAuth, includeCredentials, allowRefresh);
}

export function requestAuthVoid(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
  includeCredentials = false,
  allowRefresh = true,
) {
  return requestVoid(path, init, settings, includeAuth, includeCredentials, allowRefresh);
}

export function createLinkRequest(
  request: CreateLinkRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestJson<LinkResponse>(
    '/urls',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
  );
}

export function loginRequest(
  request: AuthCredentialsRequest,
  settings: ApiSettings = loadSettings(),
) {
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

export function registerRequest(
  request: AuthCredentialsRequest,
  settings: ApiSettings = loadSettings(),
) {
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

export function refreshTokensRequest(settings: ApiSettings = loadSettings()) {
  return requestJson<AuthResponse>(
    '/auth/refresh',
    {
      method: 'POST',
    },
    settings,
    false,
    true,
  );
}

export function logoutSessionRequest(settings: ApiSettings = loadSettings()) {
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

export function getCurrentUserRequest(settings: ApiSettings = loadSettings()) {
  return requestJson<UserProfileResponse>(
    '/auth/me',
    {
      method: 'GET',
    },
    settings,
  );
}

export function requestPasswordResetRequest(
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

export function confirmPasswordResetRequest(
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

export function requestEmailVerificationRequest(
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

export function confirmEmailVerificationRequest(
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

export function completeGithubLoginRequest(
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

export function getAccountProfileRequest(settings: ApiSettings = loadSettings()) {
  return requestJson<AccountProfileResponse>(
    '/auth/account',
    {
      method: 'GET',
    },
    settings,
  );
}

export function changePasswordRequest(
  request: PasswordChangeRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestVoid(
    '/auth/account/password',
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
    settings,
    true,
  );
}

export function getAdminOverviewRequest(settings: ApiSettings = loadSettings()) {
  return requestJson<AdminOverviewResponse>(
    '/admin/overview',
    {
      method: 'GET',
    },
    settings,
  );
}

export function listLinksRequest(
  limit: number = 10,
  settings: ApiSettings = loadSettings(),
  creator?: string | null,
) {
  const creatorQuery = creator?.trim();
  const creatorSuffix = creatorQuery ? `&creator=${encodeURIComponent(creatorQuery)}` : '';
  return requestJson<LinkResponse[]>(
    `/urls?limit=${encodeURIComponent(String(limit))}${creatorSuffix}`,
    { method: 'GET' },
    settings,
  );
}

export function getLinkRequest(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse>(
    `/urls/${encodeURIComponent(code)}`,
    { method: 'GET' },
    settings,
  );
}

export function getRedirectPreviewRequest(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<RedirectPreviewResponse>(
    `/urls/${encodeURIComponent(code)}/preview`,
    { method: 'GET' },
    settings,
  );
}

export function getAnalyticsSummaryRequest(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<AnalyticsSummaryResponse>(
    `/analytics/${encodeURIComponent(code)}`,
    { method: 'GET' },
    settings,
  );
}

export const createLink = createLinkRequest;
export const login = loginRequest;
export const register = registerRequest;
export const refreshTokens = refreshTokensRequest;
export const logoutSession = logoutSessionRequest;
export const getCurrentUser = getCurrentUserRequest;
export const requestPasswordReset = requestPasswordResetRequest;
export const confirmPasswordReset = confirmPasswordResetRequest;
export const requestEmailVerification = requestEmailVerificationRequest;
export const confirmEmailVerification = confirmEmailVerificationRequest;
export const completeGithubLogin = completeGithubLoginRequest;
export const getAccountProfile = getAccountProfileRequest;
export const changePassword = changePasswordRequest;
export const getAdminOverview = getAdminOverviewRequest;
export const listLinks = listLinksRequest;
export const getLink = getLinkRequest;
export const getRedirectPreview = getRedirectPreviewRequest;
export const getAnalyticsSummary = getAnalyticsSummaryRequest;
