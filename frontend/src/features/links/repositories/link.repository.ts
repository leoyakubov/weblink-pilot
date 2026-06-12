import type {
  ApiSettings,
  AnalyticsSummaryResponse,
  CreateLinkRequest,
  LinkResponse,
  RedirectPreviewResponse,
} from '@/types';
import { loadSettings } from '@/shared/services/settings';
import {
  createLinkRequest,
  getAnalyticsSummaryRequest,
  getLinkRequest,
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
) {
  return listLinksRequest(limit, settings, creator);
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

export type { AnalyticsSummaryResponse, LinkResponse, RedirectPreviewResponse };
