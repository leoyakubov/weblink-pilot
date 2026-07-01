import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BrowserSettingsResetView from './BrowserSettingsResetView.vue';

const mocks = vi.hoisted(() => ({
  clearSettingsMock: vi.fn(),
  routerReplaceMock: vi.fn(async () => undefined),
}));

vi.mock('@/shared/services/settings', () => ({
  clearSettings: mocks.clearSettingsMock,
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplaceMock,
  }),
}));

describe('BrowserSettingsResetView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('clears browser settings and returns to home', async () => {
    mount(BrowserSettingsResetView);
    await flushPromises();

    expect(mocks.clearSettingsMock).toHaveBeenCalled();
    expect(mocks.routerReplaceMock).toHaveBeenCalledWith({ name: 'home' });
  });
});
