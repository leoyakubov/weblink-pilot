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
  const alias = `smoke-${Date.now().toString(36)}`

  try {
    await page.goto(baseUrl, { waitUntil: 'networkidle' })
    await page.getByRole('heading', { name: /web link shortener/i }).waitFor()

    await page.getByLabel('Original URL').fill('https://github.com/openai')
    await page.getByRole('textbox', { name: 'Custom alias' }).fill(alias)
    await page.getByRole('button', { name: 'Shorten link' }).click()

    await page.getByText('Created link', { exact: true }).waitFor()
    await page.getByRole('link', { name: 'View details' }).waitFor()
    await page.getByRole('button', { name: 'Copy', exact: true }).waitFor()

    const shortUrlText = await page.getByText(`http://localhost:8080/r/${alias}`).textContent()
    assert(shortUrlText?.includes(`http://localhost:8080/r/${alias}`), 'Short URL was not rendered as expected')

    await page.getByRole('link', { name: 'View details' }).click()
    await page.getByRole('heading', { name: new RegExp(`details of "${alias}"`, 'i') }).waitFor()
    await page.getByRole('button', { name: 'QR code' }).waitFor()

    const detailsUrl = page.url()
    assert(detailsUrl.includes(`/link/${alias}`), `Unexpected details URL: ${detailsUrl}`)

    console.log(`Smoke test passed against ${baseUrl} using ${path.basename(executablePath)}`)
  } finally {
    await browser.close()
  }
}

main().catch(error => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
