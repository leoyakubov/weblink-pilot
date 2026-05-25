import type {
  AdminOverviewResponse,
  AuthCredentialsRequest,
  AuthResponse,
  ApiSettings,
  AnalyticsSummaryResponse,
  CreateLinkRequest,
  LinkResponse,
  RedirectPreviewResponse,
  UserProfileResponse,
} from '@/types';
import { loadSettings, normalizeBaseUrl } from '@/lib/settings';

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

function bearerAuthHeader(token: string) {
  if (!token) {
    return undefined;
  }
  return `Bearer ${token}`;
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

async function requestJson<T>(
  path: string,
  init: RequestInit = {},
  settings: ApiSettings = loadSettings(),
  includeAuth = true,
): Promise<T> {
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

  const response = await fetch(`${normalizeBaseUrl(settings.apiBaseUrl)}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  return response.json() as Promise<T>;
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
