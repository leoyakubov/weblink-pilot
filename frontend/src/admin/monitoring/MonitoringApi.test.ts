import { describe, expect, it, vi } from 'vitest';
import { getAdminMonitoring, getAdminOverview } from './MonitoringApi';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  getAdminMonitoringRequest: vi.fn(),
  getAdminOverviewRequest: vi.fn(),
}));

vi.mock('@/shared/services/http', () => ({
  getAdminMonitoringRequest: mocks.getAdminMonitoringRequest,
  getAdminOverviewRequest: mocks.getAdminOverviewRequest,
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('MonitoringApi', () => {
  it('delegates monitoring calls to HTTP services', () => {
    getAdminOverview(settings);
    getAdminMonitoring(settings);

    expect(mocks.getAdminOverviewRequest).toHaveBeenCalledWith(settings);
    expect(mocks.getAdminMonitoringRequest).toHaveBeenCalledWith(settings);
  });
});
