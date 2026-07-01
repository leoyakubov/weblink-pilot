import { describe, expect, it, vi } from 'vitest';
import {
  createLink,
  getAiLinkMetadata,
  getLink,
  getLinkCreatorOptions,
  getRedirectPreview,
  listLinks,
  regenerateAiLinkMetadata,
} from './LinksApi';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  calls: new Map<string, ReturnType<typeof vi.fn>>(),
  httpMock(name: string) {
    const mock = vi.fn((...args: unknown[]) => ({ name, args }));
    this.calls.set(name, mock);
    return mock;
  },
}));

vi.mock('@/shared/services/http', () => ({
  createLinkRequest: mocks.httpMock('createLinkRequest'),
  getAiLinkMetadataRequest: mocks.httpMock('getAiLinkMetadataRequest'),
  getLinkCreatorOptionsRequest: mocks.httpMock('getLinkCreatorOptionsRequest'),
  getLinkRequest: mocks.httpMock('getLinkRequest'),
  getRedirectPreviewRequest: mocks.httpMock('getRedirectPreviewRequest'),
  listLinksRequest: mocks.httpMock('listLinksRequest'),
  regenerateAiLinkMetadataRequest: mocks.httpMock('regenerateAiLinkMetadataRequest'),
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('LinksApi', () => {
  it('delegates link calls to HTTP services', () => {
    createLink({ originalUrl: 'https://example.com', customAlias: 'example' }, settings);
    listLinks(10, settings, 'admin', 'ADMIN', 'active');
    getLink('redis', settings);
    getAiLinkMetadata('redis', settings);
    regenerateAiLinkMetadata('redis', settings);
    getRedirectPreview('redis', settings);
    getLinkCreatorOptions(settings);

    expect(mocks.calls.get('createLinkRequest')).toHaveBeenCalledWith(
      { originalUrl: 'https://example.com', customAlias: 'example' },
      settings,
    );
    expect(mocks.calls.get('listLinksRequest')).toHaveBeenCalledWith(
      10,
      settings,
      'admin',
      'ADMIN',
      'active',
    );
    expect(mocks.calls.get('getLinkRequest')).toHaveBeenCalledWith('redis', settings);
    expect(mocks.calls.get('getAiLinkMetadataRequest')).toHaveBeenCalledWith('redis', settings);
    expect(mocks.calls.get('regenerateAiLinkMetadataRequest')).toHaveBeenCalledWith(
      'redis',
      settings,
    );
    expect(mocks.calls.get('getRedirectPreviewRequest')).toHaveBeenCalledWith('redis', settings);
    expect(mocks.calls.get('getLinkCreatorOptionsRequest')).toHaveBeenCalledWith(settings);
  });
});
