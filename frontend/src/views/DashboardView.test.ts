import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardView from './DashboardView.vue'

const mocks = vi.hoisted(() => ({
  listLinksMock: vi.fn(),
  getLinkMock: vi.fn(),
  getAnalyticsSummaryMock: vi.fn(),
  authState: {
    currentUser: { username: 'admin', role: 'ADMIN' } as null | { username: string; role: string },
  },
  openMock: vi.fn(),
  routeState: {
    params: {},
    query: {},
  },
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => mocks.routeState,
  useRouter: () => ({
    replace: vi.fn(),
  }),
}))

vi.mock('@/lib/auth', () => ({
  isAdminUser: () => mocks.authState.currentUser?.role === 'ADMIN',
}))

vi.mock('@/lib/api', () => ({
  buildApiBaseUrl: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  getAnalyticsSummary: mocks.getAnalyticsSummaryMock,
  getLink: mocks.getLinkMock,
  listLinks: mocks.listLinksMock,
}))

vi.mock('@/lib/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
  }),
}))

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' }
    mocks.routeState.params = {}
    mocks.routeState.query = {}
    vi.stubGlobal('open', mocks.openMock)
    mocks.listLinksMock.mockResolvedValue([
      {
        code: 'github-org',
        shortUrl: 'http://localhost:8080/r/github-org',
        qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
        originalUrl: 'https://github.com/orgs/github-org',
        createdAt: '2026-05-23T11:00:00Z',
        expiresAt: null,
        clickCount: 3,
        ownerUsername: 'admin',
      },
    ])
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/orgs/github-org',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
      ownerUsername: 'admin',
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
  })

  it('loads analytics and opens the QR modal', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Inspect clicks for any short code.')
    expect(wrapper.text()).toContain('Open preview JSON')
    expect(wrapper.text()).toContain('github-org')
    expect(wrapper.text()).toContain('US')

    const buttons = wrapper.findAll('button')
    await buttons.find(button => button.text().includes('Open QR'))?.trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('QR code')
    expect(document.body.textContent).toContain('github-org')

    await buttons.find(button => button.text().includes('preview JSON'))?.trigger('click')
    expect(mocks.openMock).toHaveBeenCalled()
  })
})
