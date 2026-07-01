import { describe, expect, it, vi } from 'vitest';
import { listAdminUsers, listAdminUsersPage } from './AdminApi';
import type { ApiSettings } from '@/shared/types/api';

const listAdminUsersRequest = vi.hoisted(() => vi.fn());
const listAdminUsersPageRequest = vi.hoisted(() => vi.fn());

vi.mock('@/shared/services/http', () => ({
  listAdminUsersPageRequest,
  listAdminUsersRequest,
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('AdminApi', () => {
  it('delegates admin user calls to HTTP services', () => {
    listAdminUsers(settings);
    listAdminUsersPage(2, 10, settings);

    expect(listAdminUsersRequest).toHaveBeenCalledWith(settings);
    expect(listAdminUsersPageRequest).toHaveBeenCalledWith(2, 10, settings);
  });
});
