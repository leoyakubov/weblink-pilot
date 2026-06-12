import type { ApiSettings, AdminOverviewResponse } from '@/types';
import { loadSettings } from '@/shared/services/settings';
import { getAdminOverviewRequest } from '@/shared/services/http';

export function getAdminOverview(settings: ApiSettings = loadSettings()) {
  return getAdminOverviewRequest(settings);
}

export type { AdminOverviewResponse };
