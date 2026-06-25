import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardView from '@/features/links/pages/DashboardView.vue';

const mocks = vi.hoisted(() => ({
  listLinksMock: vi.fn(),
  getAnalyticsSummaryMock: vi.fn(),
  getAnalyticsDetailsMock: vi.fn(),
  ApiRequestError: class ApiRequestError extends Error {
    status: number;

    constructor(message: string, status: number) {
      super(message);
      this.name = 'ApiRequestError';
      this.status = status;
    }
  },
  authState: {
    currentUser: { username: 'admin', role: 'ADMIN' } as null | { username: string; role: string },
  },
  openMock: vi.fn(),
  routeState: {
    params: { code: 'github-org' },
    query: {},
  },
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => mocks.routeState,
  useRouter: () => ({
    replace: vi.fn(),
  }),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  isAdminUser: () => mocks.authState.currentUser?.role === 'ADMIN',
}));

vi.mock('@/shared/services/http', () => ({
  ApiRequestError: mocks.ApiRequestError,
  buildApiBaseUrl: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  getAnalyticsDetails: mocks.getAnalyticsDetailsMock,
  getAnalyticsSummary: mocks.getAnalyticsSummaryMock,
  listLinks: mocks.listLinksMock,
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
}));

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };
    mocks.routeState.params = { code: 'github-org' };
    mocks.routeState.query = {};
    vi.stubGlobal('open', mocks.openMock);
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
    mocks.getAnalyticsDetailsMock.mockResolvedValue({
      code: 'github-org',
      timelineByDay: [
        {
          bucket: '2026-05-23',
          totalClicks: 3,
          redirectClicks: 2,
          qrScans: 1,
          uniqueVisitors: 2,
        },
      ],
      timelineByHour: [
        {
          bucket: '2026-05-23 11:00',
          totalClicks: 3,
          redirectClicks: 2,
          qrScans: 1,
          uniqueVisitors: 2,
        },
      ],
      browserBreakdown: [{ label: 'CHROME', clicks: 3 }],
      deviceBreakdown: [{ label: 'DESKTOP', clicks: 3 }],
      referrerBreakdown: [{ label: 'news.ycombinator.com', clicks: 3 }],
      recentEvents: [
        {
          clickedAt: '2026-05-23T11:05:00Z',
          eventSource: 'REDIRECT',
          referrer: 'https://news.ycombinator.com',
          country: 'US',
          browserFamily: 'CHROME',
          deviceType: 'DESKTOP',
        },
      ],
      sourceTrendByDay: [
        {
          bucket: '2026-05-23',
          totalClicks: 3,
          redirectClicks: 2,
          qrScans: 1,
          uniqueVisitors: 2,
        },
      ],
      visitorTrendByDay: [
        {
          bucket: '2026-05-23',
          totalClicks: 3,
          redirectClicks: 2,
          qrScans: 1,
          uniqueVisitors: 2,
        },
      ],
    });
  });

  it('loads detailed analytics for the route code', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    });
    await flushPromises();

    expect(wrapper.text()).toContain('Analytics for "github-org"');
    expect(wrapper.text()).toContain('Interaction summary');
    expect(wrapper.text()).toContain('Redirects vs QR scans');
    expect(wrapper.text()).toContain('Latest captured context');
    expect(wrapper.text()).toContain('Country distribution');
    expect(wrapper.text()).toContain('Timeline by day/hour');
    expect(wrapper.text()).toContain('Browser breakdown');
    expect(wrapper.text()).toContain('Device breakdown');
    expect(wrapper.text()).toContain('Referrer breakdown');
    expect(wrapper.text()).toContain('Recent interactions');
    expect(wrapper.text()).toContain('Unique vs returning visitors');
    expect(wrapper.text()).toContain('QR vs redirect trend');
    expect(wrapper.text()).toContain('Refresh');
    expect(wrapper.text()).toContain('US');
    expect(wrapper.text()).not.toContain('Basic link');
    expect(wrapper.text()).not.toContain('Link details');
    expect(wrapper.text()).not.toContain('All analytics');
    expect(wrapper.text()).not.toContain('Target URL');
    expect(wrapper.text()).not.toContain('Open preview JSON');
    expect(wrapper.text()).not.toContain('Open QR');
    expect(mocks.getAnalyticsSummaryMock).toHaveBeenCalledWith('github-org', expect.any(Object));
    expect(mocks.getAnalyticsDetailsMock).toHaveBeenCalledWith('github-org', expect.any(Object));
  });

  it('shows an analytics access message when analytics are forbidden', async () => {
    mocks.getAnalyticsSummaryMock.mockRejectedValueOnce(
      new mocks.ApiRequestError('Forbidden', 403),
    );

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    });
    await flushPromises();

    expect(wrapper.text()).toContain(
      'Analytics are available only to the link owner or an admin user.',
    );
    expect(wrapper.text()).not.toContain('Interaction summary');
    expect(mocks.getAnalyticsDetailsMock).toHaveBeenCalledWith('github-org', expect.any(Object));
  });
});
