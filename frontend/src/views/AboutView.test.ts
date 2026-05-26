import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AboutView from './AboutView.vue';

const mocks = vi.hoisted(() => ({
  saveSettingsMock: vi.fn(),
  settingsState: {
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  },
}));

vi.mock('@/lib/settings', () => ({
  loadSettings: () => ({ ...mocks.settingsState }),
  saveSettings: mocks.saveSettingsMock,
}));

describe('AboutView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.settingsState.apiBaseUrl = 'http://localhost:8080/api/v1';
    mocks.settingsState.authToken = '';
  });

  it('renders product details and saves backend settings', async () => {
    const wrapper = mount(AboutView);
    await flushPromises();

    expect(wrapper.text()).toContain('Built like a small SaaS, not a classroom demo.');
    expect(wrapper.text()).toContain('Vue 3');
    expect(wrapper.text()).toContain('Demo accounts');

    const input = wrapper.get('input[type="url"]');
    await input.setValue('http://localhost:9090/api/v1');
    await wrapper.get('button[type="button"]').trigger('click');
    await flushPromises();

    expect(mocks.saveSettingsMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:9090/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(wrapper.text()).toContain('Saved for this browser');
  });
});
