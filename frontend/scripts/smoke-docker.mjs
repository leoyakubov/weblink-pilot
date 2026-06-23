import { launchHeadlessBrowser } from './browser-paths.mjs'
import path from 'node:path'
import process from 'node:process'

const baseUrl = process.env.SMOKE_BASE_URL ?? 'http://localhost:8081'

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

async function main() {
  const { browser, executablePath } = await launchHeadlessBrowser()

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
