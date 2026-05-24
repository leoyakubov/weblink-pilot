import type { ApiSettings } from '@/types'

const STORAGE_KEY = 'weblinkpilot.frontend.settings'

const defaultApiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export function defaultSettings(): ApiSettings {
  return {
    apiBaseUrl: normalizeBaseUrl(defaultApiBaseUrl),
    authToken: '',
  }
}

export function loadSettings(): ApiSettings {
  if (typeof window === 'undefined') {
    return defaultSettings()
  }

  const fallback = defaultSettings()
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return fallback
  }

  try {
    const parsed = JSON.parse(raw) as Partial<ApiSettings>
    return {
      apiBaseUrl: normalizeBaseUrl(parsed.apiBaseUrl ?? fallback.apiBaseUrl),
      authToken: parsed.authToken ?? fallback.authToken,
    }
  } catch {
    return fallback
  }
}

export function saveSettings(settings: ApiSettings) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
    apiBaseUrl: normalizeBaseUrl(settings.apiBaseUrl),
    authToken: settings.authToken,
  }))
}

export function normalizeBaseUrl(value: string) {
  return value.trim().replace(/\/+$/, '')
}
