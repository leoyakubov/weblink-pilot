import type {
  AiLinkMetadataResponse,
  ApiSettings,
  CreateLinkRequest,
  LinkCreatorOptionResponse,
  LinkResponse,
  PaginatedResponse,
  RedirectPreviewResponse,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import {
  createLinkRequest,
  getAiLinkMetadataRequest,
  getLinkRequest,
  getLinkCreatorOptionsRequest,
  getRedirectPreviewRequest,
  listLinksRequest,
  listLinksPageRequest,
  regenerateAiLinkMetadataRequest,
} from '@/shared/services/http';

export function createLink(request: CreateLinkRequest, settings: ApiSettings = loadSettings()) {
  return createLinkRequest(request, settings);
}

export function listLinks(
  limit: number,
  settings: ApiSettings = loadSettings(),
  creator?: string | null,
  ownerRole?: string | null,
  expiration?: string | null,
) {
  return listLinksRequest(limit, settings, creator, ownerRole, expiration);
}

export function listLinksPage(
  page: number,
  size: number,
  settings: ApiSettings = loadSettings(),
  creator?: string | null,
  ownerRole?: string | null,
  expiration?: string | null,
) {
  return listLinksPageRequest(page, size, settings, creator, ownerRole, expiration);
}

export function getLink(code: string, settings: ApiSettings = loadSettings()) {
  return getLinkRequest(code, settings);
}

export function getAiLinkMetadata(code: string, settings: ApiSettings = loadSettings()) {
  return getAiLinkMetadataRequest(code, settings);
}

export function regenerateAiLinkMetadata(code: string, settings: ApiSettings = loadSettings()) {
  return regenerateAiLinkMetadataRequest(code, settings);
}

export function getRedirectPreview(code: string, settings: ApiSettings = loadSettings()) {
  return getRedirectPreviewRequest(code, settings);
}

export function getLinkCreatorOptions(settings: ApiSettings = loadSettings()) {
  return getLinkCreatorOptionsRequest(settings);
}

export type {
  AiLinkMetadataResponse,
  LinkCreatorOptionResponse,
  PaginatedResponse,
  LinkResponse,
  RedirectPreviewResponse,
};
