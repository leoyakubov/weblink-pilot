import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MonitoringView from '@/features/monitoring/pages/MonitoringView.vue';

const mocks = vi.hoisted(() => ({
  getAdminOverviewMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  routerPushMock: vi.fn(),
  settingsState: {
    apiBaseUrl: '/api/v1',
    authToken: '',
    refreshToken: '',
  },
}));

vi.mock('@/features/monitoring/repositories/monitoring.repository', () => ({
  getAdminOverview: mocks.getAdminOverviewMock,
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({ ...mocks.settingsState }),
  saveSettings: mocks.saveSettingsMock,
  normalizeBaseUrl: (value: string) => value.trim().replace(/\/+$/, ''),
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPushMock,
  }),
}));

describe('MonitoringView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.settingsState.apiBaseUrl = '/api/v1';
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
    expect(wrapper.text()).toContain('anonymous links');
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

  it('saves backend browser settings and opens reset flow', async () => {
    const wrapper = mount(MonitoringView);
    await flushPromises();

    const input = wrapper.get('input[type="url"]');
    await input.setValue('http://localhost:9090/api/v1');
    await wrapper.get('[data-testid="save-settings"]').trigger('click');
    await flushPromises();

    expect(mocks.saveSettingsMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:9090/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(wrapper.text()).toContain('Saved for this browser');

    await wrapper.get('[data-testid="reset-browser-settings"]').trigger('click');
    expect(mocks.routerPushMock).toHaveBeenCalledWith({ name: 'settings-reset' });
  });
});
