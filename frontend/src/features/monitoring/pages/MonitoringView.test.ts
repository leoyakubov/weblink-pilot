import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MonitoringView from '@/features/monitoring/pages/MonitoringView.vue';

const mocks = vi.hoisted(() => ({
  getAdminOverviewMock: vi.fn(),
  getAdminMonitoringMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  routerPushMock: vi.fn(),
  settingsState: {
    apiBaseUrl: '/api/v1',
    authToken: '',
    refreshToken: '',
  },
}));

vi.mock('@/features/monitoring/repositories/monitoring.repository', () => ({
  getAdminMonitoring: mocks.getAdminMonitoringMock,
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
    mocks.getAdminMonitoringMock.mockResolvedValue({
      metrics: [
        {
          group: 'JVM memory',
          name: 'Heap used',
          value: '128.0 MB',
          unit: 'bytes',
          description: 'Current heap memory used.',
        },
        {
          group: 'Threads',
          name: 'Live threads',
          value: '24',
          unit: 'threads',
          description: 'Currently live JVM threads.',
        },
        {
          group: 'Service counters',
          name: 'Links created',
          value: '4',
          unit: 'events',
          description: 'Short-link creation events.',
        },
      ],
      health: [
        { name: 'Database', status: 'UP', detail: 'PostgreSQL' },
        { name: 'Disk space', status: 'UP', detail: '1024.0 MB free' },
        { name: 'Redis', status: 'DISABLED', detail: 'Local cache provider is active.' },
      ],
      configuration: [
        {
          name: 'Active profiles',
          value: 'local',
          description: 'Spring profiles active for this runtime.',
        },
        {
          name: 'Spring env/configprops',
          value: 'hidden',
          description: 'Not exposed directly to avoid leaking secrets.',
        },
      ],
    });
  });

  it('renders the admin overview metrics', async () => {
    const wrapper = mount(MonitoringView);
    await flushPromises();

    expect(wrapper.text()).toContain('Admin monitoring');
    expect(wrapper.text()).not.toContain('Application metrics');
    expect(wrapper.text()).not.toContain('Total users');
    expect(wrapper.text()).not.toContain('Admin users');
    expect(wrapper.text()).toContain('JVM and service metrics');
    expect(wrapper.text()).toContain('Heap used');
    expect(wrapper.text()).toContain('Live threads');
    expect(wrapper.text()).toContain('Links created');
  });

  it('renders live backend and local stack links', async () => {
    const wrapper = mount(MonitoringView);
    await flushPromises();

    expect(wrapper.text()).toContain('Health checks');
    expect(wrapper.text()).toContain('Configuration');
    expect(wrapper.text()).toContain('Service endpoints');
    expect(wrapper.text()).toContain('Swagger UI');
    expect(wrapper.text()).toContain('Database');
    expect(wrapper.text()).toContain('PostgreSQL');
    expect(wrapper.find('.status.success').exists()).toBe(true);
    expect(wrapper.find('.status.warning').exists()).toBe(true);
    expect(wrapper.text()).toContain('Active profiles');
    expect(wrapper.text()).toContain('hidden');
    expect(wrapper.find('a[href="/swagger-ui/index.html"]').exists()).toBe(true);

    expect(wrapper.text()).toContain('Frontend');
    expect(wrapper.text()).toContain('Backend API');
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
