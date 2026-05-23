import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LinkView from './LinkView.vue'

const mocks = vi.hoisted(() => ({
  getLinkMock: vi.fn(),
  getRedirectPreviewMock: vi.fn(),
  getAnalyticsSummaryMock: vi.fn(),
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  copyTextMock: vi.fn(),
  replaceMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { code: 'github-org' },
  }),
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}))

vi.mock('@/lib/api', () => ({
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
  getAnalyticsSummary: mocks.getAnalyticsSummaryMock,
  getLink: mocks.getLinkMock,
  getRedirectPreview: mocks.getRedirectPreviewMock,
}))

vi.mock('@/lib/clipboard', () => ({
  copyText: mocks.copyTextMock,
}))

vi.mock('@/lib/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    username: 'admin',
    password: 'admin123',
  }),
}))

describe('LinkView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads details, preview, and analytics for the selected code', async () => {
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
    })

    mocks.getRedirectPreviewMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com/docs',
      status: 302,
      locationHeader: 'https://github.com/docs',
    })

    mocks.getAnalyticsSummaryMock.mockResolvedValue({
      code: 'github-org',
      totalClicks: 3,
      redirectClicks: 2,
      qrScans: 1,
      uniqueVisitors: 2,
      lastClickedAt: '2026-05-23T11:05:00Z',
      lastReferrer: 'https://news.ycombinator.com',
      lastBrowserFamily: 'CHROME',
      lastDeviceType: 'DESKTOP',
      topCountries: [{ country: 'US', clicks: 3 }],
    })

    const wrapper = mount(LinkView)

    await flushPromises()

    expect(mocks.getLinkMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      username: 'admin',
      password: 'admin123',
    })
    expect(mocks.getRedirectPreviewMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      username: 'admin',
      password: 'admin123',
    })
    expect(mocks.getAnalyticsSummaryMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      username: 'admin',
      password: 'admin123',
    })

    expect(wrapper.text()).toContain('Code: github-org')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('https://github.com/docs')
    expect(wrapper.text()).toContain('Copy QR URL')
    expect(wrapper.text()).toContain('US')
    expect(wrapper.text()).toContain('https://news.ycombinator.com')
  })
})
