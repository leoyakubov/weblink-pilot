import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from './HomeView.vue'

const mocks = vi.hoisted(() => ({
  createLinkMock: vi.fn(),
  copyTextMock: vi.fn(),
  getCurrentUserMock: vi.fn(),
  loginMock: vi.fn(),
  registerMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
}))

vi.mock('@/lib/api', () => ({
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
  createLink: mocks.createLinkMock,
  getCurrentUser: mocks.getCurrentUserMock,
  login: mocks.loginMock,
  register: mocks.registerMock,
}))

vi.mock('@/lib/clipboard', () => ({
  copyText: mocks.copyTextMock,
}))

vi.mock('@/lib/settings', () => ({
  defaultSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
  }),
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
  }),
  saveSettings: mocks.saveSettingsMock,
}))

describe('HomeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('creates a random-code link when the alias is left blank', async () => {
    mocks.createLinkMock.mockResolvedValue({
      code: 'abc1234',
      shortUrl: 'http://localhost:8080/r/abc1234',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/abc1234/qr',
      originalUrl: 'https://github.com/docs/getting-started',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: null,
    })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })

    expect((wrapper.get('input[placeholder="github-org"]').element as HTMLInputElement).value).toBe('')
    await wrapper.get('input[type="url"]').setValue(' https://github.com/docs/getting-started ')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(mocks.createLinkMock).toHaveBeenCalledWith(
      {
        originalUrl: 'https://github.com/docs/getting-started',
        customAlias: undefined,
        expiresAt: null,
      },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: '',
      },
    )
    expect(mocks.saveSettingsMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Created abc1234 successfully')
    expect(wrapper.text()).toContain('View details page')
    expect(wrapper.text()).toContain('Copy QR URL')
    expect(wrapper.text()).toContain('http://localhost:8080/r/abc1234')
  })

  it('creates a link and shows the created-link actions when a custom alias is provided', async () => {
    mocks.createLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/docs/getting-started',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: 'admin',
    })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })

    await wrapper.get('input[type="url"]').setValue(' https://github.com/docs/getting-started ')
    await wrapper.get('input[placeholder="github-org"]').setValue(' github-org ')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(mocks.createLinkMock).toHaveBeenCalledWith(
      {
        originalUrl: 'https://github.com/docs/getting-started',
        customAlias: 'github-org',
        expiresAt: null,
      },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: '',
      },
    )
    expect(mocks.saveSettingsMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Created github-org successfully')
    expect(wrapper.text()).toContain('View details page')
    expect(wrapper.text()).toContain('Copy QR URL')
    expect(wrapper.text()).toContain('http://localhost:8080/r/github-org')
  })
})
