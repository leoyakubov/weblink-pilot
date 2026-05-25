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
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
  }),
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
});
