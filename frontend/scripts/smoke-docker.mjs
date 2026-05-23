import { chromium } from 'playwright-core'
import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const baseUrl = process.env.SMOKE_BASE_URL ?? 'http://localhost:8081'

function browserCandidates() {
  if (process.env.PLAYWRIGHT_BROWSER_PATH) {
    return [process.env.PLAYWRIGHT_BROWSER_PATH]
  }

  const candidates = []

  if (process.platform === 'win32') {
    candidates.push(
      'C:/Program Files/Google/Chrome/Application/chrome.exe',
      'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
      'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
      'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    )
  } else if (process.platform === 'darwin') {
    candidates.push(
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
      '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    )
  } else {
    candidates.push(
      '/usr/bin/google-chrome',
      '/usr/bin/google-chrome-stable',
      '/usr/bin/chromium',
      '/usr/bin/chromium-browser',
      '/usr/bin/microsoft-edge',
    )
  }

  return candidates
}

function findBrowserExecutable() {
  for (const candidate of browserCandidates()) {
    if (candidate && fs.existsSync(candidate)) {
      return candidate
    }
  }

  return null
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

async function main() {
  const executablePath = findBrowserExecutable()
  if (!executablePath) {
    throw new Error(
      'No browser executable found. Install Chrome/Edge locally or set PLAYWRIGHT_BROWSER_PATH to a browser executable.',
    )
  }

  const browser = await chromium.launch({
    executablePath,
    headless: true,
  })

  const page = await browser.newPage()

  try {
    await page.goto(baseUrl, { waitUntil: 'networkidle' })
    await page.getByRole('heading', { name: /short links that feel like a product/i }).waitFor()

    await page.getByLabel('Original URL').fill('https://github.com/openai')
    await page.getByLabel('Custom alias').fill('github-openai')
    await page.getByRole('button', { name: 'Create link' }).click()

    await page.getByText('Created github-openai successfully').waitFor()
    await page.getByRole('link', { name: 'View details page' }).waitFor()
    await page.getByRole('button', { name: 'Copy short URL' }).waitFor()

    const shortUrlText = await page.getByText('http://localhost:8080/r/github-openai').textContent()
    assert(shortUrlText?.includes('http://localhost:8080/r/github-openai'), 'Short URL was not rendered as expected')

    await page.getByRole('link', { name: 'View details page' }).click()
    await page.getByRole('heading', { name: /code: github-openai/i }).waitFor()
    await page.getByText('Copy QR URL').waitFor()

    const detailsUrl = page.url()
    assert(detailsUrl.includes('/link/github-openai'), `Unexpected details URL: ${detailsUrl}`)

    console.log(`Smoke test passed against ${baseUrl} using ${path.basename(executablePath)}`)
  } finally {
    await browser.close()
  }
}

main().catch(error => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
