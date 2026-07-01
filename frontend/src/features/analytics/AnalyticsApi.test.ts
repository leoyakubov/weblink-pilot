import { describe, expect, it, vi } from 'vitest';
import { getAnalyticsDetails, getAnalyticsSummary } from './AnalyticsApi';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  getAnalyticsDetailsRequest: vi.fn(),
  getAnalyticsSummaryRequest: vi.fn(),
}));

vi.mock('@/shared/services/http', () => ({
  getAnalyticsDetailsRequest: mocks.getAnalyticsDetailsRequest,
  getAnalyticsSummaryRequest: mocks.getAnalyticsSummaryRequest,
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('AnalyticsApi', () => {
  it('delegates analytics calls to HTTP services', () => {
    getAnalyticsSummary('redis', settings);
    getAnalyticsDetails('redis', settings);

    expect(mocks.getAnalyticsSummaryRequest).toHaveBeenCalledWith('redis', settings);
    expect(mocks.getAnalyticsDetailsRequest).toHaveBeenCalledWith('redis', settings);
  });
});
