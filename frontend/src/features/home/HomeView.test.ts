import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HomeView from '@/features/home/HomeView.vue';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  createLinkMock: vi.fn(),
  listLinksPageMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
}));

vi.mock('@/account/AuthSession', () => ({
  authState: mocks.authState,
}));

vi.mock('@/shared/services/http', () => ({
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
}));

vi.mock('@/features/links/LinksApi', () => ({
  createLink: mocks.createLinkMock,
  listLinksPage: mocks.listLinksPageMock,
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
    document.body.innerHTML = '';
    mocks.authState.currentUser = null;
    mocks.listLinksPageMock.mockResolvedValue(pageOfLinks([]));
  });

  function pageOfLinks(content: unknown[], overrides = {}) {
    return {
      content,
      page: 0,
      size: 5,
      totalElements: content.length,
      totalPages: content.length ? 1 : 0,
      first: true,
      last: true,
      ...overrides,
    };
  }

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
    expect(wrapper.text()).toContain('Create once, share anywhere');
    expect(wrapper.text()).toContain('Start instantly');
    expect(wrapper.text()).toContain('Keep your links');
    expect(wrapper.text()).toContain('Latest links');
    expect(wrapper.text()).toContain('Guest mode (ready for anonymous links)');
  });

  it('creates a random-code link when the alias is left blank', async () => {
    mocks.createLinkMock.mockResolvedValue({
      code: 'abc1234',
      shortUrl: 'http://localhost:8080/r/abc1234',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/abc1234/qr',
      originalUrl: 'https://github.com/leoyakubov/weblink-pilot',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 0,
      ownerUsername: null,
    });

    const wrapper = mountHome();
    await flushPromises();

    expect((wrapper.get('input[type="url"]').element as HTMLInputElement).value).toBe(
      'https://github.com/leoyakubov/weblink-pilot',
    );
    expect((wrapper.get('input[type="text"]').element as HTMLInputElement).value).toBe('');

    await wrapper
      .get('input[type="url"]')
      .setValue(' https://github.com/leoyakubov/weblink-pilot ');
    await wrapper.get('input[type="text"]').setValue('');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.createLinkMock).toHaveBeenCalledWith(
      {
        originalUrl: 'https://github.com/leoyakubov/weblink-pilot',
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
    expect(wrapper.text()).not.toContain('Created abc1234 successfully');
    expect(document.body.textContent).toContain('Created link');
    expect(document.body.textContent).toContain('View details');
    expect(document.body.textContent).toContain('http://localhost:8080/r/abc1234');
  });

  it('opens the existing-link modal for the same anonymous full URL', async () => {
    mocks.listLinksPageMock.mockResolvedValue(
      pageOfLinks([
        {
          code: 'docs',
          shortUrl: 'http://localhost:8080/r/docs',
          qrCodeUrl: 'http://localhost:8080/api/v1/urls/docs/qr',
          originalUrl: 'https://github.com/leoyakubov/weblink-pilot',
          createdAt: '2026-05-23T11:00:00Z',
          expiresAt: null,
          clickCount: 2,
          ownerUsername: null,
        },
      ]),
    );

    const wrapper = mountHome();
    await flushPromises();

    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.createLinkMock).not.toHaveBeenCalled();
    expect(wrapper.text()).not.toContain('This full URL already has a short link');
    expect(document.body.textContent).toContain('Shortened link exists');
    expect(document.body.textContent).toContain(
      'This URL already has a short link for anonymous demo.',
    );
    expect(document.body.textContent).toContain('http://localhost:8080/r/docs');
  });

  it('shows signed-in status and owned links when the user is authenticated', async () => {
    mocks.authState.currentUser = {
      username: 'admin',
      role: 'ADMIN',
    };

    mocks.listLinksPageMock.mockResolvedValue(
      pageOfLinks(
        [
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
        ],
        { totalElements: 6, totalPages: 2, last: false },
      ),
    );

    const wrapper = mountHome();
    await flushPromises();

    expect(wrapper.text()).toContain('Signed in as admin (ADMIN)');
    expect(wrapper.text()).toContain('Latest links');
    expect(wrapper.text()).toContain('admin');
    expect(wrapper.text()).toContain('Details');
    expect(wrapper.text()).toContain('Analytics');
    expect(wrapper.text()).toContain('Page 1 of 2');
  });

  it('loads the next recent links page from home pagination', async () => {
    mocks.listLinksPageMock.mockResolvedValue(
      pageOfLinks(
        [
          {
            code: 'github-org',
            shortUrl: 'http://localhost:8080/r/github-org',
            qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
            originalUrl: 'https://github.com/orgs/github-org',
            createdAt: '2026-05-23T11:00:00Z',
            expiresAt: null,
            clickCount: 3,
            ownerUsername: null,
          },
        ],
        { totalElements: 6, totalPages: 2, last: false },
      ),
    );

    const wrapper = mountHome();
    await flushPromises();

    const nextButton = wrapper.findAll('button').find((button) => button.text().includes('Next'));
    expect(nextButton).toBeDefined();
    await nextButton?.trigger('click');
    await flushPromises();

    expect(mocks.listLinksPageMock).toHaveBeenLastCalledWith(1, 5, expect.any(Object));
  });
});
