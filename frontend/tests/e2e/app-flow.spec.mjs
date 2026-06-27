import { test } from 'node:test';
import assert from 'node:assert/strict';
import { installApiRoutes, openPage } from './helpers.mjs';

const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

function apiUrl(path) {
  return `http://localhost:8080/api/v1${path}`;
}

test('guest can create a link and open the details page', async () => {
  const { browser, page } = await openPage();
  let created = false;

  try {
    await installApiRoutes(page, {
      'POST /api/v1/urls': async () => {
        created = true;

        return {
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'openai-docs',
            shortUrl: 'http://localhost:8080/r/openai-docs',
            qrCodeUrl: apiUrl('/urls/openai-docs/qr'),
            originalUrl: 'https://openai.com/docs',
            createdAt: '2026-06-12T10:00:00Z',
            expiresAt: null,
            clickCount: 0,
            ownerUsername: null,
          }),
        };
      },
      'GET /api/v1/urls?limit=5': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(
          created
            ? [
                {
                  code: 'openai-docs',
                  shortUrl: 'http://localhost:8080/r/openai-docs',
                  qrCodeUrl: apiUrl('/urls/openai-docs/qr'),
                  originalUrl: 'https://openai.com/docs',
                  createdAt: '2026-06-12T10:00:00Z',
                  expiresAt: null,
                  clickCount: 0,
                  ownerUsername: null,
                },
              ]
            : [],
        ),
      }),
      'GET /api/v1/urls/openai-docs/preview': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'openai-docs',
          shortUrl: 'http://localhost:8080/r/openai-docs',
          targetUrl: 'https://openai.com/docs',
          status: 302,
          locationHeader: 'https://openai.com/docs',
        }),
      }),
      'GET /api/v1/urls/openai-docs': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'openai-docs',
          shortUrl: 'http://localhost:8080/r/openai-docs',
          qrCodeUrl: apiUrl('/urls/openai-docs/qr'),
          originalUrl: 'https://openai.com/docs',
          createdAt: '2026-06-12T10:00:00Z',
          expiresAt: null,
          clickCount: 0,
          ownerUsername: null,
        }),
      }),
      'GET /api/v1/analytics/openai-docs': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'openai-docs',
          totalClicks: 0,
          redirectClicks: 0,
          qrScans: 0,
          uniqueVisitors: 0,
          lastClickedAt: null,
          lastReferrer: null,
          lastBrowserFamily: null,
          lastDeviceType: null,
          topCountries: [],
        }),
      }),
      'GET /api/v1/analytics/openai-docs/details': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'openai-docs',
          timelineByDay: [],
          timelineByHour: [],
          browserBreakdown: [],
          deviceBreakdown: [],
          referrerBreakdown: [],
          recentEvents: [],
          sourceTrendByDay: [],
          visitorTrendByDay: [],
        }),
      }),
    });

    await page.goto(baseUrl, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor();

    await page.getByLabel('Original URL').fill('https://openai.com/docs');
    await page.getByRole('textbox', { name: 'Custom alias' }).fill('openai-docs');
    await page.getByRole('button', { name: 'Shorten link' }).click();

    await page.getByText('Created link').waitFor();
    await page.getByRole('link', { name: 'View details' }).click();
    await page.getByRole('heading', { name: /details of "openai-docs"/i }).waitFor();

    assert.match(page.url(), /\/link\/openai-docs$/);
    await page.getByRole('button', { name: 'Open', exact: true }).waitFor();
  } finally {
    await browser.close();
  }
});

test('signed-in user can log in and open the account page', async () => {
  const { browser, page } = await openPage();

  try {
    await installApiRoutes(page, {
      'POST /api/v1/auth/login': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'jwt-token',
          username: 'alice',
          role: 'USER',
        }),
      }),
      'GET /api/v1/auth/me': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          username: 'alice',
          role: 'USER',
        }),
      }),
      'GET /api/v1/auth/account': async () => ({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          username: 'alice',
          role: 'USER',
          email: 'alice@example.com',
          emailVerified: true,
          createdAt: '2026-06-12T10:00:00Z',
          lastLoginAt: '2026-06-12T11:00:00Z',
          socialIdentities: [],
        }),
      }),
    });

    await page.goto(`${baseUrl}/auth/signin`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /sign in/i }).waitFor();

    await page.getByLabel('Username').fill('alice');
    await page.locator('input[type="password"]').fill('Password1');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await page.locator('.account-name', { hasText: 'alice' }).waitFor();
    await page.goto(`${baseUrl}/account`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /account details/i }).waitFor();

    assert.match(page.url(), /\/account$/);
    await page.getByText('alice@example.com').waitFor();
  } finally {
    await browser.close();
  }
});
