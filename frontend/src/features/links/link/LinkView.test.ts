import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LinkView from '@/features/links/link/LinkView.vue';

const mocks = vi.hoisted(() => ({
  getLinkMock: vi.fn(),
  getAiLinkMetadataMock: vi.fn(),
  regenerateAiLinkMetadataMock: vi.fn(),
  getRedirectPreviewMock: vi.fn(),
  ApiRequestError: class ApiRequestError extends Error {
    status: number;

    constructor(message: string, status: number) {
      super(message);
      this.name = 'ApiRequestError';
      this.status = status;
    }
  },
  buildApiBaseUrlMock: vi.fn((path: string) => `http://localhost:8080/api/v1${path}`),
  copyTextMock: vi.fn(),
  replaceMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({
    params: { code: 'github-org' },
  }),
  useRouter: () => ({
    replace: mocks.replaceMock,
  }),
}));

vi.mock('@/shared/services/http', () => ({
  ApiRequestError: mocks.ApiRequestError,
  buildApiBaseUrl: mocks.buildApiBaseUrlMock,
}));

vi.mock('@/features/links/LinksApi', () => ({
  getAiLinkMetadata: mocks.getAiLinkMetadataMock,
  getLink: mocks.getLinkMock,
  getRedirectPreview: mocks.getRedirectPreviewMock,
  regenerateAiLinkMetadata: mocks.regenerateAiLinkMetadataMock,
}));

vi.mock('@/shared/composables/CopyAction', () => ({
  createCopyAction: () => ({
    copy: mocks.copyTextMock,
    isCopied: () => false,
  }),
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: '',
    refreshToken: '',
  }),
}));

describe('LinkView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads details and QR actions for the selected code', async () => {
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
      ownerUsername: null,
    });
    mocks.getAiLinkMetadataMock.mockResolvedValue({
      code: 'github-org',
      status: 'READY',
      provider: 'stub',
      promptVersion: 'link-metadata-v1',
      title: 'Main Docs',
      summary: 'Helpful project documentation.',
      category: 'Documentation',
      tags: ['docs', 'project'],
      icon: 'docs',
      suggestedAlias: 'main-docs',
      errorMessage: null,
      updatedAt: '2026-05-23T11:01:00Z',
      completedAt: '2026-05-23T11:01:00Z',
    });

    mocks.getRedirectPreviewMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      status: 302,
      locationHeader: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    });

    const wrapper = mount(LinkView);

    await flushPromises();

    expect(mocks.getLinkMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(mocks.getAiLinkMetadataMock).toHaveBeenCalledWith('github-org', {
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });

    expect(wrapper.text()).toContain('Details of "github-org"');
    expect(wrapper.text()).toContain('Main details');
    expect(wrapper.text()).toContain('QR code');
    expect(wrapper.text()).toContain('Analytics');
    expect(wrapper.text()).toContain('AI enrichment');
    expect(wrapper.text()).toContain('Main Docs');
    expect(wrapper.text()).toContain(
      'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    );
    expect(wrapper.text()).toContain('Copy QR');
    expect(wrapper.text()).not.toContain('Basic analytics');
  });

  it('shows link details without loading inline analytics', async () => {
    mocks.getLinkMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      qrCodeUrl: 'http://localhost:8080/api/v1/urls/github-org/qr',
      originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      createdAt: '2026-05-23T11:00:00Z',
      expiresAt: null,
      clickCount: 3,
      ownerUsername: null,
    });
    mocks.getAiLinkMetadataMock.mockResolvedValue({
      code: 'github-org',
      status: 'PENDING',
      provider: 'stub',
      promptVersion: 'link-metadata-v1',
      title: null,
      summary: null,
      category: null,
      tags: [],
      icon: null,
      suggestedAlias: null,
      errorMessage: null,
      updatedAt: null,
      completedAt: null,
    });

    mocks.getRedirectPreviewMock.mockResolvedValue({
      code: 'github-org',
      shortUrl: 'http://localhost:8080/r/github-org',
      targetUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
      status: 302,
      locationHeader: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    });

    const wrapper = mount(LinkView);
    await flushPromises();

    expect(wrapper.text()).toContain('Details of "github-org"');
    expect(wrapper.text()).toContain(
      'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
    );
  });
});
