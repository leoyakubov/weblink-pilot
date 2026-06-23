import { ensureFrontendDependencies } from './ensure-frontend-deps.mjs';
import { spawnSync, spawn } from 'node:child_process';
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import process from 'node:process';
import { setTimeout as delay } from 'node:timers/promises';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
ensureFrontendDependencies(frontendRoot);
const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');
const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173';

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
    const requestUrl = new URL(req.url ?? '/', baseUrl);
    const requestPath = decodeURIComponent(requestUrl.pathname);
    const candidatePath = path.join(rootDir, requestPath === '/' ? 'index.html' : requestPath.slice(1));
    const filePath = fs.existsSync(candidatePath) && fs.statSync(candidatePath).isFile()
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
    server.listen(4173, '127.0.0.1', () => {
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
      // keep retrying until the static server is ready
    }

    await delay(500);
  }

  throw new Error(`Timed out waiting for ${url}`);
}

function buildTempOutputDir() {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'weblink-pilot-e2e-build-'));
}

async function main() {
  const tempOutDir = buildTempOutputDir();
  const build = spawnSync(
    process.execPath,
    [viteBin, 'build', '--configLoader', 'runner', '--outDir', tempOutDir, '--emptyOutDir', 'false'],
    {
      cwd: frontendRoot,
      stdio: 'inherit',
      shell: false,
    },
  );

  if ((build.status ?? 1) !== 0) {
    process.exitCode = build.status ?? 1;
    return;
  }

  const server = await startStaticServer(tempOutDir);

  const stopServer = () => {
    server.close();
  };

  process.on('SIGINT', () => {
    stopServer();
    process.exit(130);
  });
  process.on('SIGTERM', () => {
    stopServer();
    process.exit(143);
  });

  try {
    await waitForBaseUrl(baseUrl);

    const testFiles = ['tests/e2e/app-flow.spec.mjs', 'tests/e2e/route-smoke.spec.mjs'];
    process.env.E2E_BASE_URL = baseUrl;

    const result = spawn(process.execPath, ['--test', ...testFiles], {
      cwd: frontendRoot,
      env: process.env,
      stdio: 'inherit',
      shell: false,
    });

    const exitCode = await new Promise((resolve) => {
      result.on('exit', (code) => resolve(code ?? 1));
    });

    process.exitCode = exitCode;
  } finally {
    stopServer();
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
