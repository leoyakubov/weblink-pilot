import type {
  ApiSettings,
  AnalyticsDetailsResponse,
  AnalyticsSummaryResponse,
  CreateLinkRequest,
  LinkCreatorOptionResponse,
  LinkResponse,
  RedirectPreviewResponse,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import {
  createLinkRequest,
  getAnalyticsDetailsRequest,
  getAnalyticsSummaryRequest,
  getLinkRequest,
  getLinkCreatorOptionsRequest,
  getRedirectPreviewRequest,
  listLinksRequest,
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

export function getLink(code: string, settings: ApiSettings = loadSettings()) {
  return getLinkRequest(code, settings);
}

export function getRedirectPreview(code: string, settings: ApiSettings = loadSettings()) {
  return getRedirectPreviewRequest(code, settings);
}

export function getAnalyticsSummary(code: string, settings: ApiSettings = loadSettings()) {
  return getAnalyticsSummaryRequest(code, settings);
}

export function getAnalyticsDetails(code: string, settings: ApiSettings = loadSettings()) {
  return getAnalyticsDetailsRequest(code, settings);
}

export function getLinkCreatorOptions(settings: ApiSettings = loadSettings()) {
  return getLinkCreatorOptionsRequest(settings);
}

export type {
  AnalyticsSummaryResponse,
  AnalyticsDetailsResponse,
  LinkCreatorOptionResponse,
  LinkResponse,
  RedirectPreviewResponse,
};
