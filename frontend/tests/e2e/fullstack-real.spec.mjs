import { test } from 'node:test';
import assert from 'node:assert/strict';
import { openPage } from './helpers.mjs';

const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

test('real backend smoke: guest creates a short link through the frontend', async (t) => {
  if (process.env.E2E_REAL_BACKEND !== '1') {
    t.skip('Set E2E_REAL_BACKEND=1 with a running backend to execute this smoke test.');
    return;
  }

  const { browser, page } = await openPage();
  const alias = `smoke-${Date.now().toString(36)}`;

  try {
    await page.goto(baseUrl, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor();

    await page.getByLabel('Original URL').fill('https://example.com/fullstack-smoke');
    await page.getByRole('textbox', { name: 'Custom alias' }).fill(alias);
    await page.getByRole('button', { name: 'Shorten link' }).click();

    await page.getByText('Created link').waitFor();
    await page.getByRole('link', { name: 'View details' }).click();
    await page.getByRole('heading', { name: new RegExp(`details of "${alias}"`, 'i') }).waitFor();

    assert.match(page.url(), new RegExp(`/link/${alias}$`));
    await page.getByRole('button', { name: 'Copy', exact: true }).waitFor();
  } finally {
    await browser.close();
  }
});
