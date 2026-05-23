import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { buildApiBaseUrl, createLink, getAnalyticsSummary, getRedirectPreview, listLinks } from './api'
import type { ApiSettings } from '@/types'

const settings: ApiSettings = {
  apiBaseUrl: 'http://localhost:8080/api/v1/',
  username: 'admin',
  password: 'admin123',
}

beforeEach(() => {
  vi.stubGlobal('btoa', (value: string) => Buffer.from(value, 'binary').toString('base64'))
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('api helpers', () => {
  it('builds a backend URL without duplicate slashes', () => {
    expect(buildApiBaseUrl('/urls/demo4/qr', settings)).toBe('http://localhost:8080/api/v1/urls/demo4/qr')
  })

  it('sends basic auth and serializes create link payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls')

      const headers = new Headers(init?.headers)
      expect(headers.get('Accept')).toBe('application/json')
      expect(headers.get('Content-Type')).toBe('application/json')
      expect(headers.get('Authorization')).toBe(`Basic ${Buffer.from('admin:admin123').toString('base64')}`)
      expect(init?.body).toBe(JSON.stringify({
        originalUrl: 'https://github.com',
        customAlias: 'github-org',
        expiresAt: null,
      }))

      return new Response(JSON.stringify({
        code: 'github-org',
        shortUrl: 'http://localhost:8080/r/github-org',
        qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
        originalUrl: 'https://github.com',
        createdAt: '2026-05-22T14:00:00Z',
        expiresAt: null,
        clickCount: 0,
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    vi.stubGlobal('fetch', fetchMock)

    await expect(createLink({
      originalUrl: 'https://github.com',
      customAlias: 'github-org',
      expiresAt: null,
    }, settings)).resolves.toEqual({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com',
      createdAt: '2026-05-22T14:00:00Z',
      expiresAt: null,
      clickCount: 0,
    })
  })

  it('loads redirect preview from the public endpoint', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls/github-org/preview')
      return new Response(JSON.stringify({
        code: 'github-org',
        shortUrl: 'http://localhost:8080/r/github-org',
        targetUrl: 'https://github.com',
        status: 302,
        locationHeader: 'https://github.com',
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    vi.stubGlobal('fetch', fetchMock)

    await expect(getRedirectPreview('github-org', settings)).resolves.toEqual({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com',
      status: 302,
      locationHeader: 'https://github.com',
    })
  })

  it('loads analytics summary from the backend', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/analytics/github-org')
      return new Response(JSON.stringify({
        code: 'github-org',
        totalClicks: 7,
        redirectClicks: 5,
        qrScans: 2,
        uniqueVisitors: 3,
        lastClickedAt: '2026-05-22T14:00:00Z',
        lastReferrer: 'https://news.ycombinator.com',
        lastBrowserFamily: 'Chrome',
        lastDeviceType: 'Desktop',
        topCountries: [{ country: 'Ukraine', clicks: 4 }],
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    vi.stubGlobal('fetch', fetchMock)

    await expect(getAnalyticsSummary('github-org', settings)).resolves.toEqual({
      code: 'github-org',
      totalClicks: 7,
      redirectClicks: 5,
      qrScans: 2,
      uniqueVisitors: 3,
      lastClickedAt: '2026-05-22T14:00:00Z',
      lastReferrer: 'https://news.ycombinator.com',
      lastBrowserFamily: 'Chrome',
      lastDeviceType: 'Desktop',
      topCountries: [{ country: 'Ukraine', clicks: 4 }],
    })
  })

  it('lists recent links from the backend', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls?limit=5')
      return new Response(JSON.stringify([
        {
          code: 'two',
          shortUrl: 'http://localhost:8080/r/two',
          qrCodeUrl: 'http://localhost:8080/api/v1/urls/two/qr',
          originalUrl: 'https://example.com/two',
          createdAt: '2026-05-22T15:00:00Z',
          expiresAt: null,
          clickCount: 2,
        },
      ]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    vi.stubGlobal('fetch', fetchMock)

    await expect(listLinks(5, settings)).resolves.toEqual([
      {
        code: 'two',
        shortUrl: 'http://localhost:8080/r/two',
        qrCodeUrl: 'http://localhost:8080/api/v1/urls/two/qr',
        originalUrl: 'https://example.com/two',
        createdAt: '2026-05-22T15:00:00Z',
        expiresAt: null,
        clickCount: 2,
      },
    ])
  })
})
