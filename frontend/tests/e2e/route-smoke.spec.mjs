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

test('guest can open the public pages and see guard redirects', async () => {
  const { browser, page } = await openPage();

  try {
    await page.goto(baseUrl, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /short links, qr, analytics\./i }).waitFor();

    await page.goto(`${baseUrl}/about`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /built like a small saas, not a classroom demo\./i }).waitFor();

    await page.goto(`${baseUrl}/auth/signin`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();

    await page.goto(`${baseUrl}/auth/signup`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign up/i }).waitFor();
    await page.getByLabel('Email').waitFor();

    await page.goto(`${baseUrl}/account`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();
    assert.match(page.url(), /\/auth\/signin$/);

    await page.goto(`${baseUrl}/monitoring`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /short links, qr, analytics\./i }).waitFor();
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
    await page.getByRole('button', { name: 'Send reset link' }).click();
    await page.getByText('reset link was sent').waitFor();

    await page.goto(`${baseUrl}/auth/verify-email/request`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /verify email/i }).waitFor();
    await page.getByLabel('Email').fill('alice@example.com');
    await page.getByRole('button', { name: 'Send verification email' }).click();
    await page.getByText('Verification email sent').waitFor();

    await page.goto(`${baseUrl}/auth/reset-password?token=reset-token`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /set new password/i }).waitFor();
    await page.getByLabel('Reset token').fill('reset-token');
    await page.getByLabel('New password').fill('Password1');
    await page.getByRole('button', { name: 'Update password' }).click();
    await page.getByRole('heading', { name: /sign in/i }).waitFor();
    assert.match(page.url(), /\/auth\/signin$/);

    await page.goto(`${baseUrl}/auth/verify-email?token=verify-token`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /confirm email/i }).waitFor();
    await page.getByText('Your email has been verified').waitFor();

    await page.goto(`${baseUrl}/auth/github/complete?ticket=github-ticket`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /short links, qr, analytics\./i }).waitFor();
    await page.getByText('Signed in as github-user', { exact: true }).waitFor();
  } finally {
    await browser.close();
  }
});

test('signed-in user can open dashboard, history, and account routes', async () => {
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

    await page.goto(`${baseUrl}/dashboard?code=openai-docs`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /inspect clicks for any short code\./i }).waitFor();
    assert.equal(await page.getByLabel('Short code').inputValue(), 'openai-docs');
    await page.getByText('US').waitFor();

    await page.goto(`${baseUrl}/history`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /recent links from the backend\./i }).waitFor();
    await page.getByText('openai-docs').waitFor();

    await page.goto(`${baseUrl}/account`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /your profile/i }).waitFor();
    await page.getByText('alice@example.com').waitFor();
    const linkedAccount = page.locator('.linked-account').first();
    await linkedAccount.waitFor();
    const linkedAccountText = await linkedAccount.textContent();
    assert.ok(linkedAccountText);
    assert.match(linkedAccountText, /GITHUB/i);
  } finally {
    await browser.close();
  }
});

test('admin can open monitoring and filter dashboard links by creator', async () => {
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
      'GET /api/v1/urls?limit=8': async (request, url) => {
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
      'GET /api/v1/urls?limit=8&creator=alice': async (request, url) => {
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
      'GET /api/v1/urls/github-org/preview': async () =>
        jsonResponse({
          code: 'github-org',
          shortUrl: 'http://localhost:8080/r/github-org',
          targetUrl: 'https://github.com/orgs/github-org',
          status: 302,
          locationHeader: 'https://github.com/orgs/github-org',
        }),
    });

    await page.goto(`${baseUrl}/dashboard?code=github-org`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /inspect clicks for any short code\./i }).waitFor();
    await page.getByLabel('Creator filter').waitFor();
    assert.equal(await page.getByLabel('Short code').inputValue(), 'github-org');

    await page.getByLabel('Creator filter').fill('alice');
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/api/v1/urls?limit=8&creator=alice') && response.status() === 200,
      ),
      page.getByRole('button', { name: 'Apply recent filter' }).click(),
    ]);
    assert.ok(requestedUrls.some((value) => value.includes('creator=alice')));

    await page.goto(`${baseUrl}/monitoring`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /system overview and ownership mix\./i }).waitFor();
    await page.getByText('Total users').waitFor();
    await page.getByText('Local stack').waitFor();
  } finally {
    await browser.close();
  }
});
