import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  buildApiBaseUrl,
  createLink,
  changePassword,
  confirmEmailVerification,
  confirmPasswordReset,
  getAccountProfile,
  getAdminOverview,
  getAnalyticsSummary,
  getCurrentUser,
  getRedirectPreview,
  listLinks,
  login,
  logoutSession,
  requestEmailVerification,
  requestPasswordReset,
  register,
  refreshTokens,
} from './api';
import type { ApiSettings } from '@/types';

const settings: ApiSettings = {
  apiBaseUrl: 'http://localhost:8080/api/v1/',
  authToken: 'jwt-token',
  refreshToken: 'refresh-token',
};

beforeEach(() => {
  vi.stubGlobal('btoa', (value: string) => Buffer.from(value, 'binary').toString('base64'));
  settings.apiBaseUrl = 'http://localhost:8080/api/v1/';
  settings.authToken = 'jwt-token';
  settings.refreshToken = 'refresh-token';
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('api helpers', () => {
  it('builds a backend URL without duplicate slashes', () => {
    expect(buildApiBaseUrl('/urls/demo4/qr', settings)).toBe(
      'http://localhost:8080/api/v1/urls/demo4/qr',
    );
  });

  it('sends bearer auth and serializes create link payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls');

      const headers = new Headers(init?.headers);
      expect(headers.get('Accept')).toBe('application/json');
      expect(headers.get('Content-Type')).toBe('application/json');
      expect(headers.get('Authorization')).toBe('Bearer jwt-token');
      expect(init?.body).toBe(
        JSON.stringify({
          originalUrl: 'https://github.com',
          customAlias: 'github-org',
          expiresAt: null,
        }),
      );

      return new Response(
        JSON.stringify({
          code: 'github-org',
          shortUrl: 'http://localhost:8080/r/github-org',
          qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
          originalUrl: 'https://github.com',
          createdAt: '2026-05-22T14:00:00Z',
          expiresAt: null,
          clickCount: 0,
          ownerUsername: null,
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      createLink(
        {
          originalUrl: 'https://github.com',
          customAlias: 'github-org',
          expiresAt: null,
        },
        settings,
      ),
    ).resolves.toEqual({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com',
      createdAt: '2026-05-22T14:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: null,
    });
  });

  it('loads redirect preview from the public endpoint', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls/github-org/preview');
      return new Response(
        JSON.stringify({
          code: 'github-org',
          shortUrl: 'http://localhost:8080/r/github-org',
          targetUrl: 'https://github.com',
          status: 302,
          locationHeader: 'https://github.com',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(getRedirectPreview('github-org', settings)).resolves.toEqual({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com',
      status: 302,
      locationHeader: 'https://github.com',
    });
  });

  it('loads analytics summary from the backend', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/analytics/github-org');
      return new Response(
        JSON.stringify({
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
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

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
    });
  });

  it('lists recent links from the backend', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/urls?limit=5');
      return new Response(
        JSON.stringify([
          {
            code: 'two',
            shortUrl: 'http://localhost:8080/r/two',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/two/qr',
            originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/two',
            createdAt: '2026-05-22T15:00:00Z',
            expiresAt: null,
            clickCount: 2,
            ownerUsername: null,
          },
        ]),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(listLinks(5, settings)).resolves.toEqual([
      {
        code: 'two',
        shortUrl: 'http://localhost:8080/r/two',
        qrCodeUrl: 'http://localhost:8080/api/v1/urls/two/qr',
        originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/two',
        createdAt: '2026-05-22T15:00:00Z',
        expiresAt: null,
        clickCount: 2,
        ownerUsername: null,
      },
    ]);
  });

  it('serializes login payload and reads auth response', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/login');

      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(
        JSON.stringify({
          username: 'alice',
          password: 'secret',
        }),
      );

      return new Response(
        JSON.stringify({
          token: 'new-token',
          username: 'alice',
          role: 'USER',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      login(
        {
          username: 'alice',
          password: 'secret',
        },
        settings,
      ),
    ).resolves.toEqual({
      token: 'new-token',
      username: 'alice',
      role: 'USER',
    });
  });

  it('serializes register payload and reads profile response', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/register');

      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(
        JSON.stringify({
          username: 'alice',
          password: 'secret',
          email: 'alice@example.com',
        }),
      );

      return new Response(
        JSON.stringify({
          token: 'new-token',
          username: 'alice',
          role: 'USER',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      register(
        {
          username: 'alice',
          password: 'secret',
          email: 'alice@example.com',
        },
        settings,
      ),
    ).resolves.toEqual({
      token: 'new-token',
      username: 'alice',
      role: 'USER',
    });
  });

  it('serializes password reset request payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/password-reset/request');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(JSON.stringify({ email: 'alice@example.com' }));
      return new Response(null, { status: 204 });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      requestPasswordReset({ email: 'alice@example.com' }, settings),
    ).resolves.toBeUndefined();
  });

  it('serializes password reset confirm payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/password-reset/confirm');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(
        JSON.stringify({ token: 'reset-token', password: 'Password1' }),
      );
      return new Response(null, { status: 204 });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      confirmPasswordReset({ token: 'reset-token', password: 'Password1' }, settings),
    ).resolves.toBeUndefined();
  });

  it('serializes email verification request payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/email-verification/request');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(JSON.stringify({ email: 'alice@example.com' }));
      return new Response(null, { status: 204 });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      requestEmailVerification({ email: 'alice@example.com' }, settings),
    ).resolves.toBeUndefined();
  });

  it('serializes email verification confirm payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/email-verification/confirm');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBe(JSON.stringify({ token: 'verify-token' }));
      return new Response(null, { status: 204 });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      confirmEmailVerification({ token: 'verify-token' }, settings),
    ).resolves.toBeUndefined();
  });

  it('loads account profile with linked identities', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/account');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBe('Bearer jwt-token');
      return new Response(
        JSON.stringify({
          username: 'alice',
          role: 'USER',
          email: 'alice@example.com',
          emailVerified: true,
          createdAt: '2026-05-30T10:00:00Z',
          lastLoginAt: '2026-05-30T12:00:00Z',
          socialIdentities: [{ provider: 'GITHUB', providerLogin: 'alice-github' }],
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(getAccountProfile(settings)).resolves.toEqual({
      username: 'alice',
      role: 'USER',
      email: 'alice@example.com',
      emailVerified: true,
      createdAt: '2026-05-30T10:00:00Z',
      lastLoginAt: '2026-05-30T12:00:00Z',
      socialIdentities: [{ provider: 'GITHUB', providerLogin: 'alice-github' }],
    });
  });

  it('serializes account password change payload', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/account/password');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBe('Bearer jwt-token');
      expect(headers.get('Content-Type')).toBe('application/json');
      expect(init?.body).toBe(
        JSON.stringify({
          currentPassword: 'Oldpass1',
          newPassword: 'Newpass1',
        }),
      );
      return new Response(null, { status: 204 });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      changePassword(
        {
          currentPassword: 'Oldpass1',
          newPassword: 'Newpass1',
        },
        settings,
      ),
    ).resolves.toBeUndefined();
  });

  it('serializes refresh payload and reads rotated auth response', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/refresh');

      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBeUndefined();

      return new Response(
        JSON.stringify({
          token: 'new-token',
          username: 'alice',
          role: 'USER',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(refreshTokens(settings)).resolves.toEqual({
      token: 'new-token',
      username: 'alice',
      role: 'USER',
    });
  });

  it('serializes logout payload without auth header', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/logout');

      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBeNull();
      expect(init?.credentials).toBe('include');
      expect(init?.body).toBeUndefined();

      return new Response(null, {
        status: 204,
      });
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(logoutSession(settings)).resolves.toBeUndefined();
  });

  it('loads current user profile', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/auth/me');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBe('Bearer jwt-token');
      return new Response(
        JSON.stringify({
          username: 'alice',
          role: 'USER',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(getCurrentUser(settings)).resolves.toEqual({
      username: 'alice',
      role: 'USER',
    });
  });

  it('refreshes and retries an authenticated request after a 401', async () => {
    let callCount = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      callCount += 1;
      const url = String(input);

      if (callCount === 1) {
        expect(url).toBe('http://localhost:8080/api/v1/auth/me');
        const headers = new Headers(init?.headers);
        expect(headers.get('Authorization')).toBe('Bearer jwt-token');
        return new Response(
          JSON.stringify({
            status: 401,
            message: 'expired',
          }),
          {
            status: 401,
            headers: { 'Content-Type': 'application/json' },
          },
        );
      }

      if (callCount === 2) {
        expect(url).toBe('http://localhost:8080/api/v1/auth/refresh');
        expect(init?.credentials).toBe('include');
        expect(init?.body).toBeUndefined();
        return new Response(
          JSON.stringify({
            token: 'refreshed-token',
            username: 'alice',
            role: 'USER',
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        );
      }

      expect(url).toBe('http://localhost:8080/api/v1/auth/me');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBe('Bearer refreshed-token');
      return new Response(
        JSON.stringify({
          username: 'alice',
          role: 'USER',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(getCurrentUser(settings)).resolves.toEqual({
      username: 'alice',
      role: 'USER',
    });
    expect(settings.authToken).toBe('refreshed-token');
    expect(settings.refreshToken).toBe('refresh-token');
  });

  it('loads admin overview from the backend', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe('http://localhost:8080/api/v1/admin/overview');
      const headers = new Headers(init?.headers);
      expect(headers.get('Authorization')).toBe('Bearer jwt-token');
      return new Response(
        JSON.stringify({
          totalUsers: 5,
          adminUsers: 1,
          totalLinks: 12,
          anonymousLinks: 7,
          ownedLinks: 5,
          totalClicks: 99,
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      );
    });

    vi.stubGlobal('fetch', fetchMock);

    await expect(getAdminOverview(settings)).resolves.toEqual({
      totalUsers: 5,
      adminUsers: 1,
      totalLinks: 12,
      anonymousLinks: 7,
      ownedLinks: 5,
      totalClicks: 99,
    });
  });
});
