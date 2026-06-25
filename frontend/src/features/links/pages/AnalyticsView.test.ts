import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AnalyticsView from '@/features/links/pages/AnalyticsView.vue';

const mocks = vi.hoisted(() => ({
  listLinksMock: vi.fn(),
  getAnalyticsSummaryMock: vi.fn(),
  getLinkCreatorOptionsMock: vi.fn(),
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
  routeState: {
    query: {},
  },
  replaceMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => mocks.routeState,
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  isAdminUser: () => mocks.authState.currentUser?.role === 'ADMIN',
}));

vi.mock('@/shared/services/http', () => ({
  ApiRequestError: mocks.ApiRequestError,
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  getAnalyticsSummary: mocks.getAnalyticsSummaryMock,
  getLinkCreatorOptions: mocks.getLinkCreatorOptionsMock,
  listLinks: mocks.listLinksMock,
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
}));

describe('AnalyticsView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };
    mocks.routeState.query = {};
    mocks.listLinksMock.mockResolvedValue([
      {
        code: 'redis',
        shortUrl: 'http://localhost:8080/r/redis',
        qrCodeUrl: 'http://localhost:8080/api/v1/urls/redis/qr',
        originalUrl: 'https://redis.io/docs/latest/develop/',
        createdAt: '2026-05-23T11:00:00Z',
        expiresAt: null,
        clickCount: 4,
        ownerUsername: 'user',
        ownerRole: 'USER',
      },
    ]);
    mocks.getLinkCreatorOptionsMock.mockResolvedValue([
      { username: 'anonymous', role: 'ANONYMOUS' },
      { username: 'admin', role: 'ADMIN' },
      { username: 'user', role: 'USER' },
    ]);
    mocks.getAnalyticsSummaryMock.mockResolvedValue({
      code: 'redis',
      totalClicks: 4,
      redirectClicks: 3,
      qrScans: 1,
      uniqueVisitors: 2,
      lastClickedAt: '2026-05-23T11:05:00Z',
      lastReferrer: null,
      lastBrowserFamily: 'CHROME',
      lastDeviceType: 'DESKTOP',
      topCountries: [{ country: 'US', clicks: 4 }],
    });
  });

  it('loads link analytics overview rows', async () => {
    const wrapper = mount(AnalyticsView);
    await flushPromises();

    expect(wrapper.text()).toContain('Analytics by link');
    expect(wrapper.text()).toContain('Owner group');
    expect(wrapper.text()).toContain('redis');
    expect(wrapper.text()).toContain('Total');
    expect(wrapper.text()).toContain('Redirects');
    expect(wrapper.text()).toContain('QR scans');
    expect(wrapper.text()).toContain('Detailed analytics');
    expect(mocks.listLinksMock).toHaveBeenCalledWith(20, expect.any(Object), '', '');
    expect(mocks.getAnalyticsSummaryMock).toHaveBeenCalledWith('redis', expect.any(Object));
  });

  it('passes an admin creator filter to the links request', async () => {
    const wrapper = mount(AnalyticsView);
    await flushPromises();

    await wrapper.get('select').setValue('users');
    await wrapper.findAll('select')[1].setValue('user');
    await wrapper
      .findAll('button')
      .find((button) => button.text().includes('Apply filters'))
      ?.trigger('click');
    await flushPromises();

    expect(mocks.listLinksMock).toHaveBeenLastCalledWith(20, expect.any(Object), 'user', '');
  });
});
