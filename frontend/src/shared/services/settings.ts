import type { ApiSettings } from '@/shared/types/api';

const STORAGE_KEY = 'weblinkpilot.frontend.settings';
const SESSION_KEY = 'weblinkpilot.frontend.session';
const SESSION_HINT_KEY = 'weblinkpilot.frontend.session.active';

const defaultApiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

function getStorage<T extends Storage>(storageName: 'localStorage' | 'sessionStorage'): T | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    return window[storageName] as T;
  } catch {
    return null;
  }
}

export function defaultSettings(): ApiSettings {
  return {
    apiBaseUrl: normalizeBaseUrl(defaultApiBaseUrl),
    authToken: '',
    refreshToken: '',
  };
}

export function loadSettings(): ApiSettings {
  const fallback = defaultSettings();
  const localStorage = getStorage<Storage>('localStorage');
  const sessionStorage = getStorage<Storage>('sessionStorage');
  const raw = localStorage?.getItem(STORAGE_KEY);
  const sessionToken = sessionStorage?.getItem(SESSION_KEY);
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
  const localStorage = getStorage<Storage>('localStorage');
  const sessionStorage = getStorage<Storage>('sessionStorage');

  localStorage?.setItem(
    STORAGE_KEY,
    JSON.stringify({
      apiBaseUrl: normalizeBaseUrl(settings.apiBaseUrl),
    }),
  );
  sessionStorage?.setItem(SESSION_KEY, settings.authToken);
  if (settings.authToken) {
    sessionStorage?.setItem(SESSION_HINT_KEY, '1');
  }
}

export function clearSettings() {
  const localStorage = getStorage<Storage>('localStorage');
  const sessionStorage = getStorage<Storage>('sessionStorage');

  localStorage?.removeItem(STORAGE_KEY);
  sessionStorage?.removeItem(SESSION_KEY);
  sessionStorage?.removeItem(SESSION_HINT_KEY);
}

export function markSessionActive() {
  getStorage<Storage>('sessionStorage')?.setItem(SESSION_HINT_KEY, '1');
}

export function clearSessionActive() {
  getStorage<Storage>('sessionStorage')?.removeItem(SESSION_HINT_KEY);
}

export function hasSessionActiveHint() {
  return getStorage<Storage>('sessionStorage')?.getItem(SESSION_HINT_KEY) === '1';
}

export function normalizeBaseUrl(value: string) {
  return value.trim().replace(/\/+$/, '');
}
