import fs from 'node:fs';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import process from 'node:process';
import { chromium } from 'playwright-core';

function windowsBrowserCandidates() {
  return [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    'C:/Users/dev/AppData/Local/Google/Chrome/Application/chrome.exe',
    'C:/Users/dev/AppData/Local/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    '/mnt/c/Program Files/Google/Chrome/Application/chrome.exe',
    '/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    '/mnt/c/Users/dev/AppData/Local/Google/Chrome/Application/chrome.exe',
    '/mnt/c/Users/dev/AppData/Local/Microsoft/Edge/Application/msedge.exe',
    '/mnt/c/Program Files/Microsoft/Edge/Application/msedge.exe',
    '/mnt/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  ];
}

function linuxBrowserCandidates() {
  return [
    '/usr/bin/google-chrome',
    '/usr/bin/google-chrome-stable',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
    '/usr/bin/microsoft-edge',
  ];
}

function macBrowserCandidates() {
  return [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
  ];
}

export function browserCandidates() {
  if (process.env.PLAYWRIGHT_BROWSER_PATH) {
    return [process.env.PLAYWRIGHT_BROWSER_PATH];
  }

  if (process.platform === 'win32') {
    return windowsBrowserCandidates();
  }

  if (process.platform === 'darwin') {
    return macBrowserCandidates();
  }

  return [...linuxBrowserCandidates(), ...windowsBrowserCandidates()];
}

export function findBrowserExecutable() {
  for (const candidate of browserCandidates()) {
    if (candidate && fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return null;
}

function isWindowsBrowserExecutable(executablePath) {
  return executablePath.toLowerCase().endsWith('.exe');
}

function isWindowsDebuggableBrowser(executablePath) {
  return process.platform !== 'win32' && isWindowsBrowserExecutable(executablePath);
}

function waitForPort(port, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now();
    const check = () => {
      const socket = net.connect({ host: '127.0.0.1', port }, () => {
        socket.end();
        resolve();
      });

      socket.on('error', () => {
        socket.destroy();
        if (Date.now() - startedAt > timeoutMs) {
          reject(new Error(`Timed out waiting for debugging port ${port}`));
          return;
        }
        setTimeout(check, 250);
      });
    };

    check();
  });
}

async function launchChromiumWithPipe(executablePath) {
  const browser = await chromium.launch({
    executablePath,
    headless: true,
  });
  return { browser, cleanup: async () => {} };
}

async function launchWindowsBrowserViaCdp(executablePath) {
  const debugPort = 9222 + Math.floor(Math.random() * 500);
  const profileDir = fs.mkdtempSync(path.join(os.tmpdir(), 'weblink-pilot-browser-'));
  const browserArgs = [
    '--headless=new',
    '--remote-debugging-address=127.0.0.1',
    `--remote-debugging-port=${debugPort}`,
    `--user-data-dir=${profileDir}`,
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-gpu',
    '--disable-dev-shm-usage',
    '--disable-extensions',
    '--disable-background-networking',
    '--disable-popup-blocking',
  ];

  const browserProcess = spawn(executablePath, browserArgs, {
    detached: false,
    stdio: 'ignore',
    shell: false,
  });

  browserProcess.unref();
  await waitForPort(debugPort);

  const browser = await chromium.connectOverCDP(`http://127.0.0.1:${debugPort}`);
  const originalClose = browser.close.bind(browser);

  const cleanup = async () => {
    try {
      if (!browserProcess.killed) {
        browserProcess.kill();
      }
    } catch {
      // Ignore cleanup errors; the browser is already detached enough for tests.
    }

    fs.rmSync(profileDir, { recursive: true, force: true });
  };

  browser.close = async () => {
    try {
      await originalClose();
    } finally {
      await cleanup();
    }
  };

  return { browser, cleanup };
}

export async function launchHeadlessBrowser() {
  const executablePath = findBrowserExecutable();
  if (!executablePath) {
    throw new Error(
      'No browser executable found. Install Chrome/Edge in the standard Windows location, install a Linux browser inside WSL, or set PLAYWRIGHT_BROWSER_PATH to an executable path.',
    );
  }

  if (isWindowsDebuggableBrowser(executablePath)) {
    return launchWindowsBrowserViaCdp(executablePath);
  }

  return launchChromiumWithPipe(executablePath);
}
