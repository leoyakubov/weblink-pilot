import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MonitoringView from './MonitoringView.vue';

const mocks = vi.hoisted(() => ({
  getAdminOverviewMock: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  getAdminOverview: mocks.getAdminOverviewMock,
}));

vi.mock('@/lib/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: '/api/v1',
    authToken: '',
  }),
  normalizeBaseUrl: (value: string) => value.trim().replace(/\/+$/, ''),
}));

describe('MonitoringView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getAdminOverviewMock.mockResolvedValue({
      totalUsers: 5,
      adminUsers: 1,
      totalLinks: 12,
      anonymousLinks: 7,
      ownedLinks: 5,
      totalClicks: 99,
    });
  });

  it('renders the admin overview metrics', async () => {
    const wrapper = mount(MonitoringView);
    await flushPromises();

    expect(wrapper.text()).toContain('Admin monitoring');
    expect(wrapper.text()).toContain('Total users');
    expect(wrapper.text()).toContain('99');
    expect(wrapper.text()).toContain('Anonymous links');
  });

  it('renders live backend and local stack links', async () => {
    const wrapper = mount(MonitoringView);
    await flushPromises();

    expect(wrapper.text()).toContain('Live backend');
    expect(wrapper.find('a[href="/actuator/health"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/actuator/info"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/actuator/metrics"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/actuator/prometheus"]').exists()).toBe(true);

    expect(wrapper.text()).toContain('Local stack');
    expect(wrapper.find('a[href="http://localhost:9090"]').exists()).toBe(true);
    expect(wrapper.find('a[href="http://localhost:3001"]').exists()).toBe(true);
  });
});
