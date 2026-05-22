import type { ApiSettings, CreateLinkRequest, LinkResponse, RedirectPreviewResponse } from '@/types'
import { loadSettings, normalizeBaseUrl } from '@/lib/settings'

function basicAuthHeader(username: string, password: string) {
  if (!username || !password) {
    return undefined
  }
  return `Basic ${btoa(`${username}:${password}`)}`
}

async function parseError(response: Response) {
  const text = await response.text()
  try {
    const body = JSON.parse(text) as { message?: string; error?: string; code?: string }
    return new Error(body.message ?? body.error ?? body.code ?? `Request failed with ${response.status}`)
  } catch {
    return new Error(text || `Request failed with ${response.status}`)
  }
}

async function requestJson<T>(path: string, init: RequestInit = {}, settings: ApiSettings = loadSettings()): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const authorization = basicAuthHeader(settings.username, settings.password)
  if (authorization) {
    headers.set('Authorization', authorization)
  }

  const response = await fetch(`${normalizeBaseUrl(settings.apiBaseUrl)}${path}`, {
    ...init,
    headers,
  })

  if (!response.ok) {
    throw await parseError(response)
  }

  return response.json() as Promise<T>
}

export function buildApiBaseUrl(path: string, settings: ApiSettings = loadSettings()) {
  return `${normalizeBaseUrl(settings.apiBaseUrl)}${path}`
}

export function createLink(request: CreateLinkRequest, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse>('/urls', {
    method: 'POST',
    body: JSON.stringify(request),
  }, settings)
}

export function getLink(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<LinkResponse>(`/urls/${encodeURIComponent(code)}`, { method: 'GET' }, settings)
}

export function getRedirectPreview(code: string, settings: ApiSettings = loadSettings()) {
  return requestJson<RedirectPreviewResponse>(`/urls/${encodeURIComponent(code)}/preview`, { method: 'GET' }, settings)
}
