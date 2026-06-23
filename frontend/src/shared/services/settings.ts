import type { ApiSettings } from '@/shared/types/api';

const STORAGE_KEY = 'weblinkpilot.frontend.settings';
const SESSION_KEY = 'weblinkpilot.frontend.session';
const SESSION_HINT_KEY = 'weblinkpilot.frontend.session.active';

const defaultApiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export function defaultSettings(): ApiSettings {
  return {
    apiBaseUrl: normalizeBaseUrl(defaultApiBaseUrl),
    authToken: '',
    refreshToken: '',
  };
}

export function loadSettings(): ApiSettings {
  if (typeof window === 'undefined') {
    return defaultSettings();
  }

  const fallback = defaultSettings();
  const raw = window.localStorage.getItem(STORAGE_KEY);
  const sessionToken = window.sessionStorage.getItem(SESSION_KEY);
  if (!raw) {
    return {
      ...fallback,
      authToken: sessionToken ?? fallback.authToken,
    };
  }

  try {
    const parsed = JSON.parse(raw) as Partial<ApiSettings>;
    return {
      apiBaseUrl: normalizeBaseUrl(parsed.apiBaseUrl ?? fallback.apiBaseUrl),
      authToken: sessionToken ?? parsed.authToken ?? fallback.authToken,
      refreshToken: fallback.refreshToken,
    };
  } catch {
    return {
      ...fallback,
      authToken: sessionToken ?? fallback.authToken,
    };
  }
}

export function saveSettings(settings: ApiSettings) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      apiBaseUrl: normalizeBaseUrl(settings.apiBaseUrl),
    }),
  );
  window.sessionStorage.setItem(SESSION_KEY, settings.authToken);
  if (settings.authToken) {
    window.sessionStorage.setItem(SESSION_HINT_KEY, '1');
  }
}

export function clearSettings() {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.removeItem(STORAGE_KEY);
  window.sessionStorage.removeItem(SESSION_KEY);
  window.sessionStorage.removeItem(SESSION_HINT_KEY);
}

export function markSessionActive() {
  if (typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.setItem(SESSION_HINT_KEY, '1');
}

export function clearSessionActive() {
  if (typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.removeItem(SESSION_HINT_KEY);
}

export function hasSessionActiveHint() {
  if (typeof window === 'undefined') {
    return false;
  }

  return window.sessionStorage.getItem(SESSION_HINT_KEY) === '1';
}

export function normalizeBaseUrl(value: string) {
  return value.trim().replace(/\/+$/, '');
}
