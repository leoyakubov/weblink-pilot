import { test } from 'node:test';
import assert from 'node:assert/strict';
import { installApiRoutes, openPage } from './helpers.mjs';

const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

function jsonResponse(body, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  };
}

function seedSession(page, token) {
  return page.addInitScript((sessionToken) => {
    window.localStorage.setItem(
      'weblinkpilot.frontend.settings',
      JSON.stringify({ apiBaseUrl: 'http://localhost:8080/api/v1' }),
    );
    window.sessionStorage.setItem('weblinkpilot.frontend.session', sessionToken);
    window.sessionStorage.setItem('weblinkpilot.frontend.session.active', '1');
  }, token);
}

function makeLink(overrides = {}) {
  return {
    code: 'github-org',
    shortUrl: 'http://localhost:8080/r/github-org',
    qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
    originalUrl: 'https://github.com/orgs/github-org',
    createdAt: '2026-05-23T11:00:00Z',
    expiresAt: null,
    clickCount: 3,
    ownerUsername: 'admin',
    ...overrides,
  };
}

function makeAnalyticsDetails(code = 'openai-docs') {
  return {
    code,
    timelineByDay: [
      { bucket: '2026-06-12', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 4 },
    ],
    timelineByHour: [
      {
        bucket: '2026-06-12 10:00',
        totalClicks: 9,
        redirectClicks: 7,
        qrScans: 2,
        uniqueVisitors: 4,
      },
    ],
    browserBreakdown: [{ label: 'CHROME', clicks: 9 }],
    deviceBreakdown: [{ label: 'DESKTOP', clicks: 9 }],
    referrerBreakdown: [{ label: 'news.ycombinator.com', clicks: 9 }],
    recentEvents: [
      {
        clickedAt: '2026-06-12T10:30:00Z',
        eventSource: 'REDIRECT',
        referrer: 'https://news.ycombinator.com',
        country: 'US',
        browserFamily: 'CHROME',
        deviceType: 'DESKTOP',
      },
    ],
    sourceTrendByDay: [
      { bucket: '2026-06-12', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 4 },
    ],
    visitorTrendByDay: [
      { bucket: '2026-06-12', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 4 },
    ],
  };
}

test('guest can open the public pages and see guard redirects', async () => {
  const { browser, page } = await openPage();

  try {
    await page.goto(baseUrl, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor();

    await page.goto(`${baseUrl}/about`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /weblinkpilot/i }).waitFor();

    await page.goto(`${baseUrl}/auth/signin`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();

    await page.goto(`${baseUrl}/auth/signup`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign up/i }).waitFor();
    await page.getByLabel('Email').waitFor();

    await page.goto(`${baseUrl}/account`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();
    assert.match(page.url(), /\/auth\/signin$/);

    await page.goto(`${baseUrl}/monitoring`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor();
    assert.match(page.url(), /\/$/);
  } finally {
    await browser.close();
  }
});

test('auth recovery routes and the GitHub callback work', async () => {
  const { browser, page } = await openPage();

  try {
    await installApiRoutes(page, {
      'POST /api/v1/auth/password-reset/request': async () => jsonResponse({}, 204),
      'POST /api/v1/auth/password-reset/confirm': async () => jsonResponse({}, 204),
      'POST /api/v1/auth/email-verification/request': async () => jsonResponse({}, 204),
      'POST /api/v1/auth/email-verification/confirm': async () => jsonResponse({}, 204),
      'POST /api/v1/auth/oauth2/github/complete': async () =>
        jsonResponse({
          token: 'jwt-token',
          username: 'github-user',
          role: 'USER',
        }),
    });

    await page.goto(`${baseUrl}/auth/forgot-password`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /reset password/i }).waitFor();
    await page.getByLabel('Email').fill('alice@example.com');
    await page.getByRole('button', { name: 'Send link' }).click();
    await page.getByText('reset link was sent').waitFor();

    await page.goto(`${baseUrl}/auth/verify-email/request`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /verify email/i }).waitFor();
    await page.getByLabel('Email').fill('alice@example.com');
    await page.getByRole('button', { name: 'Send email' }).click();
    await page.getByText('Verification email sent').waitFor();

    await page.goto(`${baseUrl}/auth/reset-password?token=reset-token`, {
      waitUntil: 'networkidle',
    });
    await page.getByRole('heading', { name: /set new password/i }).waitFor();
    await page.getByLabel('Reset token').fill('reset-token');
    await page.getByLabel('New password').fill('Password1');
    await page.getByRole('button', { name: 'Update' }).click();
    await page.getByRole('heading', { name: /sign in/i }).waitFor();
    assert.match(page.url(), /\/auth\/signin$/);

    await page.goto(`${baseUrl}/auth/verify-email?token=verify-token`, {
      waitUntil: 'networkidle',
    });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();
    await page.getByText('Email verified').waitFor();
    await page.getByText('Your email has been verified. You can sign in now.').waitFor();
    assert.match(page.url(), /\/auth\/signin$/);

    await page.goto(`${baseUrl}/auth/github/complete?ticket=github-ticket`, {
      waitUntil: 'networkidle',
    });
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor();
    await page.getByText('Signed in as github-user', { exact: true }).waitFor();
  } finally {
    await browser.close();
  }
});

test('signed-in user can open analytics, links, and account routes', async () => {
  const { browser, page } = await openPage();

  try {
    await seedSession(page, 'jwt-token');
    await installApiRoutes(page, {
      'GET /api/v1/auth/me': async () =>
        jsonResponse({
          username: 'alice',
          role: 'USER',
        }),
      'GET /api/v1/urls?limit=8': async () =>
        jsonResponse([
          makeLink({
            ownerUsername: 'alice',
            originalUrl: 'https://openai.com/docs',
            shortUrl: 'http://localhost:8080/r/openai-docs',
            code: 'openai-docs',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/openai-docs/qr',
          }),
        ]),
      'GET /api/v1/urls?limit=20': async () =>
        jsonResponse([
          makeLink({
            ownerUsername: 'alice',
            originalUrl: 'https://openai.com/docs',
            shortUrl: 'http://localhost:8080/r/openai-docs',
            code: 'openai-docs',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/openai-docs/qr',
          }),
        ]),
      'GET /api/v1/urls/openai-docs': async () =>
        jsonResponse(
          makeLink({
            ownerUsername: 'alice',
            originalUrl: 'https://openai.com/docs',
            shortUrl: 'http://localhost:8080/r/openai-docs',
            code: 'openai-docs',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/openai-docs/qr',
          }),
        ),
      'GET /api/v1/urls/openai-docs/preview': async () =>
        jsonResponse({
          code: 'openai-docs',
          shortUrl: 'http://localhost:8080/r/openai-docs',
          targetUrl: 'https://openai.com/docs',
          status: 302,
          locationHeader: 'https://openai.com/docs',
        }),
      'GET /api/v1/analytics/openai-docs': async () =>
        jsonResponse({
          code: 'openai-docs',
          totalClicks: 9,
          redirectClicks: 7,
          qrScans: 2,
          uniqueVisitors: 4,
          lastClickedAt: '2026-06-12T10:30:00Z',
          lastReferrer: 'https://news.ycombinator.com',
          lastBrowserFamily: 'CHROME',
          lastDeviceType: 'DESKTOP',
          topCountries: [{ country: 'US', clicks: 9 }],
        }),
      'GET /api/v1/analytics/openai-docs/details': async () =>
        jsonResponse(makeAnalyticsDetails('openai-docs')),
      'GET /api/v1/auth/account': async () =>
        jsonResponse({
          username: 'alice',
          role: 'USER',
          email: 'alice@example.com',
          emailVerified: true,
          createdAt: '2026-05-30T10:00:00Z',
          lastLoginAt: '2026-06-12T09:45:00Z',
          socialIdentities: [{ provider: 'GITHUB', providerLogin: 'alice-github' }],
        }),
    });

    await page.goto(`${baseUrl}/analytics/openai-docs`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /analytics for "openai-docs"/i }).waitFor();
    await page.getByText('US', { exact: true }).waitFor();

    await page.goto(`${baseUrl}/links`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { level: 1, name: /saved links/i }).waitFor();
    await page.locator('.recent-link-code', { hasText: 'openai-docs' }).waitFor();

    await page.goto(`${baseUrl}/account`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /account details/i }).waitFor();
    await page.getByText('alice@example.com').waitFor();

    await page.goto(`${baseUrl}/account/security`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /change password/i }).waitFor();
    const connectedSignIns = page.locator('section', {
      has: page.getByRole('heading', { name: /connected sign-ins/i }),
    });
    await connectedSignIns.getByRole('heading', { name: /connected sign-ins/i }).waitFor();
    await connectedSignIns.getByText('GitHub', { exact: true }).waitFor();
    await connectedSignIns.getByText('alice-github', { exact: true }).waitFor();
  } finally {
    await browser.close();
  }
});

test('admin can open monitoring and filter analytics links by creator', async () => {
  const { browser, page } = await openPage();
  const requestedUrls = [];

  try {
    await seedSession(page, 'admin-token');
    await installApiRoutes(page, {
      'GET /api/v1/auth/me': async () =>
        jsonResponse({
          username: 'admin',
          role: 'ADMIN',
        }),
      'GET /api/v1/admin/overview': async () =>
        jsonResponse({
          totalUsers: 5,
          adminUsers: 1,
          totalLinks: 12,
          anonymousLinks: 7,
          ownedLinks: 5,
          totalClicks: 99,
        }),
      'GET /api/v1/admin/monitoring': async () =>
        jsonResponse({
          metrics: [
            {
              group: 'JVM memory',
              name: 'Heap used',
              value: '128.0 MB',
              unit: 'bytes',
              description: 'Current heap memory used.',
            },
            {
              group: 'Service counters',
              name: 'Links created',
              value: '4',
              unit: 'events',
              description: 'Short-link creation events.',
            },
          ],
          health: [
            { name: 'Database', status: 'UP', detail: 'PostgreSQL' },
            { name: 'Redis', status: 'UP', detail: 'Connected' },
          ],
          configuration: [
            { name: 'Active profiles', value: 'local', description: 'Spring profiles.' },
          ],
        }),
      'GET /api/v1/admin/link-creators': async () =>
        jsonResponse([
          { username: 'anonymous', role: 'ANONYMOUS' },
          { username: 'admin', role: 'ADMIN' },
          { username: 'alice', role: 'USER' },
        ]),
      'GET /api/v1/urls?limit=20': async (request, url) => {
        requestedUrls.push(url.pathname + url.search);
        return jsonResponse([
          makeLink({
            ownerUsername: 'admin',
            originalUrl: 'https://github.com/orgs/github-org',
            code: 'github-org',
            shortUrl: 'http://localhost:8080/r/github-org',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
          }),
        ]);
      },
      'GET /api/v1/urls?limit=20&creator=alice': async (request, url) => {
        requestedUrls.push(url.pathname + url.search);
        return jsonResponse([
          makeLink({
            ownerUsername: 'alice',
            originalUrl: 'https://openai.com/docs',
            code: 'openai-docs',
            shortUrl: 'http://localhost:8080/r/openai-docs',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/openai-docs/qr',
          }),
        ]);
      },
      'GET /api/v1/analytics/openai-docs': async () =>
        jsonResponse({
          code: 'openai-docs',
          totalClicks: 9,
          redirectClicks: 7,
          qrScans: 2,
          uniqueVisitors: 4,
          lastClickedAt: '2026-06-12T10:30:00Z',
          lastReferrer: 'https://news.ycombinator.com',
          lastBrowserFamily: 'CHROME',
          lastDeviceType: 'DESKTOP',
          topCountries: [{ country: 'US', clicks: 9 }],
        }),
      'GET /api/v1/analytics/openai-docs/details': async () =>
        jsonResponse(makeAnalyticsDetails('openai-docs')),
      'GET /api/v1/urls/github-org': async () =>
        jsonResponse(
          makeLink({
            ownerUsername: 'admin',
            originalUrl: 'https://github.com/orgs/github-org',
            code: 'github-org',
            shortUrl: 'http://localhost:8080/r/github-org',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
          }),
        ),
      'GET /api/v1/analytics/github-org': async () =>
        jsonResponse({
          code: 'github-org',
          totalClicks: 3,
          redirectClicks: 2,
          qrScans: 1,
          uniqueVisitors: 2,
          lastClickedAt: '2026-05-23T11:05:00Z',
          lastReferrer: 'https://news.ycombinator.com',
          lastBrowserFamily: 'CHROME',
          lastDeviceType: 'DESKTOP',
          topCountries: [{ country: 'US', clicks: 3 }],
        }),
      'GET /api/v1/analytics/github-org/details': async () =>
        jsonResponse(makeAnalyticsDetails('github-org')),
      'GET /api/v1/urls/github-org/preview': async () =>
        jsonResponse({
          code: 'github-org',
          shortUrl: 'http://localhost:8080/r/github-org',
          targetUrl: 'https://github.com/orgs/github-org',
          status: 302,
          locationHeader: 'https://github.com/orgs/github-org',
        }),
    });

    await page.goto(`${baseUrl}/analytics?code=github-org`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /link analytics/i }).waitFor();
    await page.getByLabel('Creator').waitFor();
    await page.getByLabel('Owner group').waitFor();

    await page.getByLabel('Creator').selectOption('alice');
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/api/v1/urls?limit=20&creator=alice') &&
          response.status() === 200,
      ),
      page.getByRole('button', { name: 'Apply filters' }).click(),
    ]);
    assert.ok(requestedUrls.some((value) => value.includes('creator=alice')));

    await page.goto(`${baseUrl}/monitoring`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /admin monitoring/i }).waitFor();
    await page.getByText('Health checks').waitFor();
    await page.getByText('JVM and service metrics').waitFor();
    await page.getByText('Local stack').waitFor();
  } finally {
    await browser.close();
  }
});
