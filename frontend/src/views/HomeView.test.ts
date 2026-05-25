import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HomeView from './HomeView.vue';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  createLinkMock: vi.fn(),
  listLinksMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
}));

vi.mock('@/lib/auth', () => ({
  authState: mocks.authState,
}));

vi.mock('@/lib/api', () => ({
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
  createLink: mocks.createLinkMock,
  listLinks: mocks.listLinksMock,
}));

vi.mock('@/lib/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
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

    expect(wrapper.text()).toContain('Short links, QR, analytics.');
    expect(wrapper.text()).toContain('Quick create');
    expect(wrapper.text()).toContain('Recent links');
    expect(wrapper.text()).toContain('Guest mode ready for demo links');
  });

  it('creates a random-code link when the alias is left blank', async () => {
    mocks.createLinkMock.mockResolvedValue({
      code: 'abc1234',
      shortUrl: 'http://localhost:8080/r/abc1234',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/abc1234/qr',
      originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: null,
    });

    const wrapper = mountHome();
    await flushPromises();

    await wrapper
      .get('input[type="url"]')
      .setValue(' https://github.com/weblinkpilot/weblink-pilot/tree/main/docs ');
    await wrapper.get('input[type="text"]').setValue(' ');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.createLinkMock).toHaveBeenCalledWith(
      {
        originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
        customAlias: undefined,
        expiresAt: null,
      },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: '',
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
    expect(wrapper.text()).toContain('Your Recent Links');
    expect(wrapper.text()).toContain('admin');
    expect(wrapper.text()).toContain('Details');
    expect(wrapper.text()).toContain('Analytics');
  });
});
