import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HomeView from '@/features/links/pages/HomeView.vue';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  createLinkMock: vi.fn(),
  listLinksMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  authState: mocks.authState,
}));

vi.mock('@/shared/services/http', () => ({
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  createLink: mocks.createLinkMock,
  listLinks: mocks.listLinksMock,
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
  saveSettings: mocks.saveSettingsMock,
}));

describe('HomeView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authState.currentUser = null;
    mocks.listLinksMock.mockResolvedValue([]);
  });

  function mountHome() {
    return mount(HomeView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    });
  }

  it('renders the hero and the recent links area', async () => {
    const wrapper = mountHome();
    await flushPromises();

    expect(wrapper.text()).toContain('Link management');
    expect(wrapper.text()).toContain('Shorten link');
    expect(wrapper.text()).toContain('Guest demo links');
    expect(wrapper.text()).toContain('Recent links');
    expect(wrapper.text()).toContain('Guest mode (ready for anonymous links)');
  });

  it('creates a random-code link when the alias is left blank', async () => {
    mocks.createLinkMock.mockResolvedValue({
      code: 'abc1234',
      shortUrl: 'http://localhost:8080/r/abc1234',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/abc1234/qr',
      originalUrl: 'https://github.com/weblinkpilot/',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: null,
    });

    const wrapper = mountHome();
    await flushPromises();

    expect((wrapper.get('input[type="url"]').element as HTMLInputElement).value).toBe(
      'https://github.com/weblinkpilot/',
    );
    expect((wrapper.get('input[type="text"]').element as HTMLInputElement).value).toBe('wlpilot');

    await wrapper.get('input[type="url"]').setValue(' https://github.com/weblinkpilot/ ');
    await wrapper.get('input[type="text"]').setValue('');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.createLinkMock).toHaveBeenCalledWith(
      {
        originalUrl: 'https://github.com/weblinkpilot/',
        customAlias: undefined,
        expiresAt: null,
      },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: '',
        refreshToken: '',
      },
    );
    expect(mocks.saveSettingsMock).toHaveBeenCalled();
    expect(wrapper.text()).toContain('Created abc1234 successfully');
    expect(wrapper.text()).toContain('View details page');
    expect(wrapper.text()).toContain('Copy QR URL');
    expect(wrapper.text()).toContain('http://localhost:8080/r/abc1234');
  });

  it('shows signed-in status and owned links when the user is authenticated', async () => {
    mocks.authState.currentUser = {
      username: 'admin',
      role: 'ADMIN',
    };

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
    ]);

    const wrapper = mountHome();
    await flushPromises();

    expect(wrapper.text()).toContain('Signed in as admin (ADMIN)');
    expect(wrapper.text()).toContain('Recent links');
    expect(wrapper.text()).toContain('admin');
    expect(wrapper.text()).toContain('Details');
    expect(wrapper.text()).toContain('Analytics');
  });
});
