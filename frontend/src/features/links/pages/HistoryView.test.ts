import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HistoryView from '@/features/links/pages/HistoryView.vue';

const mocks = vi.hoisted(() => ({
  listLinksMock: vi.fn(),
  openMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
}));

vi.mock('@/features/links/repositories/link.repository', () => ({
  buildApiBaseUrl: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  listLinks: mocks.listLinksMock,
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
    vi.stubGlobal('open', mocks.openMock);
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

    expect(wrapper.text()).toContain('Link history');
    expect(wrapper.text()).toContain('Recent links');
    expect(wrapper.text()).toContain('github-org');
    expect(wrapper.text()).toContain('3 clicks');

    const buttons = wrapper.findAll('button');
    await buttons.find((button) => button.text().includes('Open QR'))?.trigger('click');
    expect(mocks.openMock).toHaveBeenCalled();
  });
});
