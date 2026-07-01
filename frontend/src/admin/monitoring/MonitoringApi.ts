import type {
  AdminMonitoringResponse,
  AdminOverviewResponse,
  ApiSettings,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import { getAdminMonitoringRequest, getAdminOverviewRequest } from '@/shared/services/http';

export function getAdminOverview(settings: ApiSettings = loadSettings()) {
  return getAdminOverviewRequest(settings);
}

export function getAdminMonitoring(settings: ApiSettings = loadSettings()) {
  return getAdminMonitoringRequest(settings);
}

export type { AdminMonitoringResponse, AdminOverviewResponse };
