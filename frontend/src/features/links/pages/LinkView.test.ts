import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LinkView from '@/features/links/pages/LinkView.vue';

const mocks = vi.hoisted(() => ({
  getLinkMock: vi.fn(),
  getRedirectPreviewMock: vi.fn(),
  getAnalyticsSummaryMock: vi.fn(),
  ApiRequestError: class ApiRequestError extends Error {
    status: number;

    constructor(message: string, status: number) {
      super(message);
      this.name = 'ApiRequestError';
      this.status = status;
    }
  },
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  copyTextMock: vi.fn(),
  replaceMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { code: 'github-org' },
  }),
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}));

vi.mock('@/shared/services/http', () => ({
  ApiRequestError: mocks.ApiRequestError,
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  getAnalyticsSummary: mocks.getAnalyticsSummaryMock,
  getLink: mocks.getLinkMock,
  getRedirectPreview: mocks.getRedirectPreviewMock,
}));

vi.mock('@/shared/composables/useCopyAction', () => ({
  useCopyAction: () => ({
    copy: mocks.copyTextMock,
    isCopied: () => false,
  }),
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
}));

describe('LinkView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads details, preview, and analytics for the selected code', async () => {
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
      ownerUsername: null,
    });

    mocks.getRedirectPreviewMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      status: 302,
      locationHeader: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    });

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
    });

    const wrapper = mount(LinkView);

    await flushPromises();

    expect(mocks.getLinkMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(mocks.getRedirectPreviewMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(mocks.getAnalyticsSummaryMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });

    expect(wrapper.text()).toContain('Code: github-org');
    expect(wrapper.text()).toContain('3');
    expect(wrapper.text()).toContain(
      'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    );
    expect(wrapper.text()).toContain('Copy QR URL');
    expect(wrapper.text()).toContain('Click breakdown');
  });

  it('shows link details when analytics are forbidden', async () => {
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
      ownerUsername: null,
    });

    mocks.getRedirectPreviewMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      status: 302,
      locationHeader: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    });

    mocks.getAnalyticsSummaryMock.mockRejectedValue(new mocks.ApiRequestError('Forbidden', 403));

    const wrapper = mount(LinkView);
    await flushPromises();

    expect(wrapper.text()).toContain(
      'Analytics are available only to the link owner or an admin user.',
    );
    expect(wrapper.text()).toContain('Code: github-org');
    expect(wrapper.text()).toContain(
      'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    );
  });
});
