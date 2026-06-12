import fs from 'node:fs';
import { chromium } from 'playwright-core';

export function findBrowserExecutable() {
  if (process.env.PLAYWRIGHT_BROWSER_PATH) {
    return process.env.PLAYWRIGHT_BROWSER_PATH;
  }

  const candidates = [];

  if (process.platform === 'win32') {
    candidates.push(
      'C:/Program Files/Google/Chrome/Application/chrome.exe',
      'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
      'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
      'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    );
  } else if (process.platform === 'darwin') {
    candidates.push(
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
      '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    );
  } else {
    candidates.push(
      '/usr/bin/google-chrome',
      '/usr/bin/google-chrome-stable',
      '/usr/bin/chromium',
      '/usr/bin/chromium-browser',
      '/usr/bin/microsoft-edge',
    );
  }

  for (const candidate of candidates) {
    if (candidate && fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return null;
}

export async function openPage() {
  const executablePath = findBrowserExecutable();
  if (!executablePath) {
    throw new Error(
      'No browser executable found. Install Chrome/Edge locally or set PLAYWRIGHT_BROWSER_PATH to a browser executable.',
    );
  }

  const browser = await chromium.launch({
    executablePath,
    headless: true,
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  return { browser, context, page };
}
