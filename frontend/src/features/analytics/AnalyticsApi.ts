import type {
  AnalyticsDetailsResponse,
  AnalyticsSummaryResponse,
  ApiSettings,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import { getAnalyticsDetailsRequest, getAnalyticsSummaryRequest } from '@/shared/services/http';

export function getAnalyticsSummary(code: string, settings: ApiSettings = loadSettings()) {
  return getAnalyticsSummaryRequest(code, settings);
}

export function getAnalyticsDetails(code: string, settings: ApiSettings = loadSettings()) {
  return getAnalyticsDetailsRequest(code, settings);
}

export type { AnalyticsSummaryResponse, AnalyticsDetailsResponse };
