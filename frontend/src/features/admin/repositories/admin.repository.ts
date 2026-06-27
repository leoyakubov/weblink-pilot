import type { AdminUserResponse, ApiSettings } from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import { listAdminUsersRequest } from '@/shared/services/http';

export function listAdminUsers(settings: ApiSettings = loadSettings()) {
  return listAdminUsersRequest(settings);
}

export type { AdminUserResponse };
