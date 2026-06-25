import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HistoryView from '@/features/links/pages/HistoryView.vue';

const mocks = vi.hoisted(() => ({
  listLinksMock: vi.fn(),
  getLinkCreatorOptionsMock: vi.fn(),
  authState: {
    currentUser: { username: 'admin', role: 'ADMIN' } as null | { username: string; role: string },
  },
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  buildApiBaseUrl: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  getLinkCreatorOptions: mocks.getLinkCreatorOptionsMock,
  listLinks: mocks.listLinksMock,
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  isAdminUser: () => mocks.authState.currentUser?.role === 'ADMIN',
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
}));

describe('HistoryView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };
    mocks.getLinkCreatorOptionsMock.mockResolvedValue([
      { username: 'anonymous', role: 'ANONYMOUS' },
      { username: 'admin', role: 'ADMIN' },
      { username: 'user', role: 'USER' },
    ]);
  });

  it('renders recent links and opens quick actions', async () => {
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

    const wrapper = mount(HistoryView, {
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

    expect(wrapper.text()).toContain('Links');
    expect(wrapper.text()).toContain('Saved links');
    expect(wrapper.text()).toContain('Latest links');
    expect(wrapper.text()).toContain('Owner group');
    expect(wrapper.text()).toContain('github-org');
    expect(wrapper.text()).toContain('3 clicks');
    expect(mocks.listLinksMock).toHaveBeenCalledWith(20, expect.any(Object), '', '');

    const buttons = wrapper.findAll('button');
    await buttons.find((button) => button.text().includes('Open QR'))?.trigger('click');
    await flushPromises();
    expect(document.body.textContent).toContain('QR code');
    expect(document.body.textContent).toContain('github-org');
  });

  it('passes selected admin filters to the links request', async () => {
    mocks.listLinksMock.mockResolvedValue([]);

    const wrapper = mount(HistoryView);
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
