import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import process from 'node:process';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';
import { ensureFrontendDependencies } from './ensure-frontend-deps.mjs';
import { launchHeadlessBrowser } from './browser-paths.mjs';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
const projectRoot = path.resolve(frontendRoot, '..');
const screenshotDir = path.join(projectRoot, 'docs', 'images');
const apiBaseUrl = 'http://localhost:8080/api/v1';
const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');

ensureFrontendDependencies(frontendRoot);

const links = [
  makeLink({
    code: 'openai-docs',
    originalUrl: 'https://platform.openai.com/docs',
    shortUrl: 'http://localhost:8080/r/openai-docs',
    qrCodeUrl: `${apiBaseUrl}/urls/openai-docs/qr`,
    createdAt: '2026-06-12T10:00:00Z',
    clickCount: 42,
    ownerUsername: 'alice',
  }),
  makeLink({
    code: 'vue-patterns',
    originalUrl: 'https://vuejs.org/guide/introduction.html',
    shortUrl: 'http://localhost:8080/r/vue-patterns',
    qrCodeUrl: `${apiBaseUrl}/urls/vue-patterns/qr`,
    createdAt: '2026-06-10T09:30:00Z',
    clickCount: 27,
    ownerUsername: 'admin',
  }),
  makeLink({
    code: 'spring-boot',
    originalUrl: 'https://spring.io/projects/spring-boot',
    shortUrl: 'http://localhost:8080/r/spring-boot',
    qrCodeUrl: `${apiBaseUrl}/urls/spring-boot/qr`,
    createdAt: '2026-06-09T15:15:00Z',
    clickCount: 18,
    ownerUsername: null,
  }),
  makeLink({
    code: 'redis-guide',
    originalUrl: 'https://redis.io/docs/latest/',
    shortUrl: 'http://localhost:8080/r/redis-guide',
    qrCodeUrl: `${apiBaseUrl}/urls/redis-guide/qr`,
    createdAt: '2026-06-08T12:45:00Z',
    clickCount: 14,
    ownerUsername: 'alice',
  }),
  makeLink({
    code: 'docker-compose',
    originalUrl: 'https://docs.docker.com/compose/',
    shortUrl: 'http://localhost:8080/r/docker-compose',
    qrCodeUrl: `${apiBaseUrl}/urls/docker-compose/qr`,
    createdAt: '2026-06-07T08:10:00Z',
    clickCount: 9,
    ownerUsername: 'admin',
  }),
];

const createdLink = makeLink({
  code: 'spring-stack',
  originalUrl: 'https://docs.spring.io/spring-boot/index.html',
  shortUrl: 'http://localhost:8080/r/spring-stack',
  qrCodeUrl: `${apiBaseUrl}/urls/spring-stack/qr`,
  createdAt: '2026-06-13T11:20:00Z',
  clickCount: 0,
  ownerUsername: null,
});

function contentTypeFor(filePath) {
  const ext = path.extname(filePath).toLowerCase();

  switch (ext) {
    case '.html':
      return 'text/html; charset=utf-8';
    case '.js':
      return 'text/javascript; charset=utf-8';
    case '.css':
      return 'text/css; charset=utf-8';
    case '.json':
      return 'application/json; charset=utf-8';
    case '.svg':
      return 'image/svg+xml';
    case '.woff':
      return 'font/woff';
    case '.woff2':
      return 'font/woff2';
    case '.ttf':
      return 'font/ttf';
    case '.eot':
      return 'application/vnd.ms-fontobject';
    case '.ico':
      return 'image/x-icon';
    case '.png':
      return 'image/png';
    default:
      return 'application/octet-stream';
  }
}

function startStaticServer(rootDir) {
  const server = http.createServer((req, res) => {
    const requestUrl = new URL(req.url ?? '/', 'http://127.0.0.1');
    const requestPath = decodeURIComponent(requestUrl.pathname);
    const candidatePath = path.join(
      rootDir,
      requestPath === '/' ? 'index.html' : requestPath.slice(1),
    );
    const filePath =
      fs.existsSync(candidatePath) && fs.statSync(candidatePath).isFile()
        ? candidatePath
        : path.join(rootDir, 'index.html');

    fs.readFile(filePath, (error, data) => {
      if (error) {
        res.statusCode = 404;
        res.end('Not found');
        return;
      }

      res.setHeader('Content-Type', contentTypeFor(filePath));
      res.end(data);
    });
  });

  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolve(server);
    });
  });
}

async function waitForBaseUrl(url) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const response = await fetch(url);
      if (response.ok || response.status === 404) {
        return;
      }
    } catch {
      // The build server may need a moment before accepting connections.
    }

    await delay(500);
  }

  throw new Error(`Timed out waiting for ${url}`);
}

function buildTempOutputDir() {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'weblink-pilot-screenshots-'));
}

function makeLink(overrides = {}) {
  return {
    code: 'openai-docs',
    shortUrl: 'http://localhost:8080/r/openai-docs',
    qrCodeUrl: `${apiBaseUrl}/urls/openai-docs/qr`,
    originalUrl: 'https://platform.openai.com/docs',
    createdAt: '2026-06-12T10:00:00Z',
    expiresAt: null,
    clickCount: 0,
    ownerUsername: null,
    ...overrides,
  };
}

function paginatedLinks(content, url) {
  const page = Number(url.searchParams.get('page') ?? '0');
  const size = Number(url.searchParams.get('size') ?? '10');
  const start = page * size;
  const pageContent = content.slice(start, start + size);
  const totalPages = content.length > 0 ? Math.ceil(content.length / size) : 0;

  return {
    content: pageContent,
    page,
    size,
    totalElements: content.length,
    totalPages,
    first: page === 0,
    last: totalPages === 0 || page >= totalPages - 1,
  };
}

function jsonResponse(body, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  };
}

function svgResponse(body) {
  return {
    status: 200,
    contentType: 'image/svg+xml',
    body,
  };
}

function linkByCode(code) {
  return [createdLink, ...links].find((link) => link.code === code) ?? null;
}

function analyticsSummary(code) {
  const totals = {
    'openai-docs': [42, 34, 8, 19],
    'vue-patterns': [27, 21, 6, 13],
    'spring-boot': [18, 15, 3, 8],
    'redis-guide': [14, 11, 3, 7],
    'docker-compose': [9, 8, 1, 5],
    'spring-stack': [0, 0, 0, 0],
  };
  const [totalClicks, redirectClicks, qrScans, uniqueVisitors] = totals[code] ?? [3, 2, 1, 2];

  return {
    code,
    totalClicks,
    redirectClicks,
    qrScans,
    uniqueVisitors,
    lastClickedAt: totalClicks ? '2026-06-13T10:30:00Z' : null,
    lastReferrer: totalClicks ? 'https://news.ycombinator.com' : null,
    lastBrowserFamily: totalClicks ? 'CHROME' : null,
    lastDeviceType: totalClicks ? 'DESKTOP' : null,
    topCountries: totalClicks
      ? [
          { country: 'US', clicks: Math.max(1, totalClicks - 8) },
          { country: 'DE', clicks: 5 },
          { country: 'UA', clicks: 3 },
        ]
      : [],
  };
}

function analyticsDetails(code) {
  return {
    code,
    timelineByDay: [
      { bucket: '2026-06-10', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 5 },
      { bucket: '2026-06-11', totalClicks: 14, redirectClicks: 12, qrScans: 2, uniqueVisitors: 8 },
      { bucket: '2026-06-12', totalClicks: 19, redirectClicks: 15, qrScans: 4, uniqueVisitors: 11 },
    ],
    timelineByHour: [
      {
        bucket: '2026-06-12 10:00',
        totalClicks: 12,
        redirectClicks: 10,
        qrScans: 2,
        uniqueVisitors: 7,
      },
      {
        bucket: '2026-06-12 11:00',
        totalClicks: 7,
        redirectClicks: 5,
        qrScans: 2,
        uniqueVisitors: 4,
      },
    ],
    browserBreakdown: [
      { label: 'CHROME', clicks: 29 },
      { label: 'SAFARI', clicks: 8 },
      { label: 'FIREFOX', clicks: 5 },
    ],
    deviceBreakdown: [
      { label: 'DESKTOP', clicks: 31 },
      { label: 'MOBILE', clicks: 11 },
    ],
    referrerBreakdown: [
      { label: 'news.ycombinator.com', clicks: 18 },
      { label: 'github.com', clicks: 13 },
      { label: 'direct', clicks: 11 },
    ],
    recentEvents: [
      {
        clickedAt: '2026-06-13T10:30:00Z',
        eventSource: 'REDIRECT',
        referrer: 'https://news.ycombinator.com',
        country: 'US',
        browserFamily: 'CHROME',
        deviceType: 'DESKTOP',
      },
      {
        clickedAt: '2026-06-13T09:55:00Z',
        eventSource: 'QR_SCAN',
        referrer: null,
        country: 'DE',
        browserFamily: 'SAFARI',
        deviceType: 'MOBILE',
      },
    ],
    sourceTrendByDay: [
      { bucket: '2026-06-10', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 5 },
      { bucket: '2026-06-11', totalClicks: 14, redirectClicks: 12, qrScans: 2, uniqueVisitors: 8 },
      { bucket: '2026-06-12', totalClicks: 19, redirectClicks: 15, qrScans: 4, uniqueVisitors: 11 },
    ],
    visitorTrendByDay: [
      { bucket: '2026-06-10', totalClicks: 9, redirectClicks: 7, qrScans: 2, uniqueVisitors: 5 },
      { bucket: '2026-06-11', totalClicks: 14, redirectClicks: 12, qrScans: 2, uniqueVisitors: 8 },
      { bucket: '2026-06-12', totalClicks: 19, redirectClicks: 15, qrScans: 4, uniqueVisitors: 11 },
    ],
  };
}

function aiMetadata(code) {
  return {
    code,
    status: 'READY',
    provider: 'stub',
    promptVersion: 'link-metadata-v1',
    title: code === 'openai-docs' ? 'OpenAI API documentation' : 'Technical reference',
    summary:
      'AI enrichment turns a raw URL into a readable title, category, tags, and suggested alias for review-friendly link details.',
    category: 'Documentation',
    tags: ['docs', 'backend', 'portfolio'],
    icon: 'docs',
    suggestedAlias: code,
    errorMessage: null,
    updatedAt: '2026-06-12T10:01:00Z',
    completedAt: '2026-06-12T10:01:00Z',
  };
}

function qrSvg(code) {
  const cells = Array.from({ length: 13 }, (_, row) =>
    Array.from({ length: 13 }, (_, col) => {
      const edge = row < 4 && col < 4;
      const edge2 = row < 4 && col > 8;
      const edge3 = row > 8 && col < 4;
      const noise = (row * 7 + col * 11 + code.length) % 5 === 0;
      return edge || edge2 || edge3 || noise;
    }),
  );
  const rects = cells
    .flatMap((row, y) =>
      row.map((filled, x) =>
        filled ? `<rect x="${28 + x * 16}" y="${28 + y * 16}" width="12" height="12" rx="2"/>` : '',
      ),
    )
    .join('');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
    <rect width="256" height="256" rx="24" fill="#f8fafc"/>
    <rect x="16" y="16" width="224" height="224" rx="18" fill="#ffffff" stroke="#bfdbfe" stroke-width="4"/>
    <g fill="#0f172a">${rects}</g>
  </svg>`;
}

async function installApiRoutes(page) {
  await page.route('http://localhost:8080/api/v1/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const pathName = url.pathname.replace('/api/v1', '');

    if (method === 'GET' && pathName === '/auth/me') {
      await route.fulfill(jsonResponse({ username: 'admin', role: 'ADMIN' }));
      return;
    }

    if (method === 'POST' && pathName === '/urls') {
      await route.fulfill(jsonResponse(createdLink));
      return;
    }

    if (method === 'GET' && pathName === '/urls') {
      await route.fulfill(jsonResponse(paginatedLinks(links, url)));
      return;
    }

    const linkMatch = pathName.match(/^\/urls\/([^/]+)$/);
    if (method === 'GET' && linkMatch) {
      const link = linkByCode(decodeURIComponent(linkMatch[1]));
      await route.fulfill(link ? jsonResponse(link) : jsonResponse({ message: 'Not found' }, 404));
      return;
    }

    const qrMatch = pathName.match(/^\/urls\/([^/]+)\/qr$/);
    if (method === 'GET' && qrMatch) {
      await route.fulfill(svgResponse(qrSvg(decodeURIComponent(qrMatch[1]))));
      return;
    }

    const previewMatch = pathName.match(/^\/urls\/([^/]+)\/preview$/);
    if (method === 'GET' && previewMatch) {
      const link = linkByCode(decodeURIComponent(previewMatch[1]));
      await route.fulfill(
        link
          ? jsonResponse({
              code: link.code,
              shortUrl: link.shortUrl,
              targetUrl: link.originalUrl,
              status: 302,
              locationHeader: link.originalUrl,
            })
          : jsonResponse({ message: 'Not found' }, 404),
      );
      return;
    }

    const metadataMatch = pathName.match(/^\/ai\/links\/([^/]+)\/metadata$/);
    if (method === 'GET' && metadataMatch) {
      await route.fulfill(jsonResponse(aiMetadata(decodeURIComponent(metadataMatch[1]))));
      return;
    }

    if (method === 'POST' && metadataMatch) {
      await route.fulfill(jsonResponse(aiMetadata(decodeURIComponent(metadataMatch[1]))));
      return;
    }

    const analyticsSummaryMatch = pathName.match(/^\/analytics\/([^/]+)$/);
    if (method === 'GET' && analyticsSummaryMatch) {
      await route.fulfill(
        jsonResponse(analyticsSummary(decodeURIComponent(analyticsSummaryMatch[1]))),
      );
      return;
    }

    const analyticsDetailsMatch = pathName.match(/^\/analytics\/([^/]+)\/details$/);
    if (method === 'GET' && analyticsDetailsMatch) {
      await route.fulfill(
        jsonResponse(analyticsDetails(decodeURIComponent(analyticsDetailsMatch[1]))),
      );
      return;
    }

    if (method === 'GET' && pathName === '/admin/overview') {
      await route.fulfill(
        jsonResponse({
          totalUsers: 5,
          adminUsers: 1,
          totalLinks: 18,
          anonymousLinks: 6,
          ownedLinks: 12,
          totalClicks: 128,
        }),
      );
      return;
    }

    if (method === 'GET' && pathName === '/admin/link-creators') {
      await route.fulfill(
        jsonResponse([
          { username: 'anonymous', role: 'ANONYMOUS' },
          { username: 'admin', role: 'ADMIN' },
          { username: 'alice', role: 'USER' },
        ]),
      );
      return;
    }

    if (method === 'GET' && pathName === '/admin/monitoring') {
      await route.fulfill(
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
              group: 'HTTP traffic',
              name: 'Successful requests',
              value: '1,284',
              unit: 'requests',
              description: 'Successful API responses in the demo window.',
            },
            {
              group: 'Service counters',
              name: 'Links created',
              value: '18',
              unit: 'events',
              description: 'Short-link creation events.',
            },
          ],
          health: [
            { name: 'Database', status: 'UP', detail: 'PostgreSQL accepting connections' },
            { name: 'Redis', status: 'UP', detail: 'Cache and counters available' },
            { name: 'Mail', status: 'UP', detail: 'Mailpit SMTP reachable' },
            { name: 'AI provider', status: 'UP', detail: 'Stub provider ready' },
          ],
          configuration: [
            { name: 'Active profiles', value: 'local', description: 'Spring profiles.' },
            { name: 'Cache mode', value: 'redis', description: 'Short-code lookup cache.' },
            {
              name: 'Email sender',
              value: 'mailpit',
              description: 'Account notification transport.',
            },
          ],
        }),
      );
      return;
    }

    await route.fulfill(
      jsonResponse({ message: `No screenshot mock for ${method} ${pathName}` }, 404),
    );
  });
}

async function seedSettings(page, { admin = false } = {}) {
  await page.addInitScript(
    ({ apiBaseUrl: baseUrl, shouldSeedAdmin }) => {
      window.localStorage.setItem(
        'weblinkpilot.frontend.settings',
        JSON.stringify({ apiBaseUrl: baseUrl }),
      );

      if (shouldSeedAdmin) {
        window.sessionStorage.setItem('weblinkpilot.frontend.session', 'admin-token');
        window.sessionStorage.setItem('weblinkpilot.frontend.session.active', '1');
      }
    },
    { apiBaseUrl, shouldSeedAdmin: admin },
  );
}

async function waitForStablePage(page) {
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts?.ready);
  await page.waitForTimeout(250);
}

async function capture(page, fileName) {
  await waitForStablePage(page);
  await page.screenshot({
    path: path.join(screenshotDir, fileName),
    fullPage: false,
  });
  console.log(`Captured docs/images/${fileName}`);
}

async function createPage(browser, { admin = false } = {}) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1050 },
    deviceScaleFactor: 1,
    reducedMotion: 'reduce',
  });
  const page = await context.newPage();
  await installApiRoutes(page);
  await seedSettings(page, { admin });
  return { context, page };
}

async function withPage(browser, options, callback) {
  const { context, page } = await createPage(browser, options);

  try {
    await callback(page);
  } finally {
    await context.close();
  }
}

async function buildFrontend(tempOutDir) {
  const build = spawnSync(
    process.execPath,
    [
      viteBin,
      'build',
      '--configLoader',
      'runner',
      '--outDir',
      tempOutDir,
      '--emptyOutDir',
      'false',
    ],
    {
      cwd: frontendRoot,
      stdio: 'inherit',
      shell: false,
    },
  );

  if ((build.status ?? 1) !== 0) {
    throw new Error(`Frontend build failed with exit code ${build.status ?? 1}`);
  }
}

async function main() {
  fs.mkdirSync(screenshotDir, { recursive: true });
  const tempOutDir = buildTempOutputDir();
  await buildFrontend(tempOutDir);

  const server = await startStaticServer(tempOutDir);
  const address = server.address();
  if (!address || typeof address === 'string') {
    throw new Error('Failed to determine the screenshot server address');
  }
  const baseUrl = `http://${address.address}:${address.port}`;

  try {
    await waitForBaseUrl(baseUrl);
    const { browser } = await launchHeadlessBrowser();

    try {
      await withPage(browser, {}, async (page) => {
        await page.goto(baseUrl, { waitUntil: 'networkidle' });
        await page.getByRole('heading', { name: /web link shortener/i }).waitFor();
        await page.getByText('Latest links').waitFor();
        await capture(page, '01-home-create-link.png');
      });

      await withPage(browser, {}, async (page) => {
        await page.goto(baseUrl, { waitUntil: 'networkidle' });
        await page.getByLabel('Original URL').fill(createdLink.originalUrl);
        await page.locator('#custom-alias').fill(createdLink.code);
        await page.getByRole('button', { name: 'Shorten link' }).click();
        await page.locator('.modal-card', { hasText: createdLink.code }).waitFor();
        await capture(page, '02-link-created-success.png');
      });

      await withPage(browser, { admin: true }, async (page) => {
        await page.goto(`${baseUrl}/link/openai-docs`, { waitUntil: 'networkidle' });
        await page.getByRole('heading', { name: /details of "openai-docs"/i }).waitFor();
        await page.getByText('AI enrichment', { exact: true }).waitFor();
        await capture(page, '03-link-details.png');
      });

      await withPage(browser, { admin: true }, async (page) => {
        await page.goto(`${baseUrl}/analytics`, { waitUntil: 'networkidle' });
        await page.getByRole('heading', { name: /link analytics/i }).waitFor();
        await page.locator('.analytics-table__row').first().waitFor();
        await capture(page, '04-analytics-overview.png');
      });

      await withPage(browser, {}, async (page) => {
        await page.goto(`${baseUrl}/auth/signin`, { waitUntil: 'networkidle' });
        await page.getByRole('heading', { name: /sign in/i }).waitFor();
        await capture(page, '05-signin.png');
      });

      await withPage(browser, { admin: true }, async (page) => {
        await page.goto(`${baseUrl}/monitoring`, { waitUntil: 'networkidle' });
        await page.getByRole('heading', { name: /admin monitoring/i }).waitFor();
        await page.getByText('Health checks').waitFor();
        await capture(page, '06-admin-monitoring.png');
      });
    } finally {
      await browser.close();
    }
  } finally {
    server.close();
    fs.rmSync(tempOutDir, { recursive: true, force: true });
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
