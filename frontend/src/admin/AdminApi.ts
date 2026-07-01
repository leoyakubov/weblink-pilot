import type { AdminUserResponse, ApiSettings } from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import { listAdminUsersPageRequest, listAdminUsersRequest } from '@/shared/services/http';

export function listAdminUsers(settings: ApiSettings = loadSettings()) {
  return listAdminUsersRequest(settings);
}

export function listAdminUsersPage(
  page: number,
  size: number,
  settings: ApiSettings = loadSettings(),
) {
  return listAdminUsersPageRequest(page, size, settings);
}

export type { AdminUserResponse };
