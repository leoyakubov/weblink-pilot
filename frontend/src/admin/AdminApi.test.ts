import { describe, expect, it, vi } from 'vitest';
import { listAdminUsers } from './AdminApi';
import type { ApiSettings } from '@/shared/types/api';

const listAdminUsersRequest = vi.hoisted(() => vi.fn());

vi.mock('@/shared/services/http', () => ({
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

    expect(listAdminUsersRequest).toHaveBeenCalledWith(settings);
  });
});
