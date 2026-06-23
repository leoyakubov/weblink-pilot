import { launchHeadlessBrowser } from '../../scripts/browser-paths.mjs';

export async function openPage() {
  const { browser } = await launchHeadlessBrowser();
  const context = await browser.newContext();
  const page = await context.newPage();

  return { browser, context, page };
}

export async function installApiRoutes(page, routeMap) {
  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const url = new URL(route.request().url());
    const key = `${route.request().method()} ${url.pathname}${url.search}`;
    const handler = routeMap[key] ?? routeMap[`${route.request().method()} ${url.pathname}`];

    if (!handler) {
      await route.fulfill({ status: 404, body: 'Not mocked' });
      return;
    }

    const result = await handler(route.request(), url);
    await route.fulfill(result);
  });
}
