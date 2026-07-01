import { describe, expect, it, vi } from 'vitest';
import { changePassword, getAccountProfile } from './AccountApi';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  changePasswordRequest: vi.fn(),
  getAccountProfileRequest: vi.fn(),
}));

vi.mock('@/shared/services/http', () => ({
  changePasswordRequest: mocks.changePasswordRequest,
  getAccountProfileRequest: mocks.getAccountProfileRequest,
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('AccountApi', () => {
  it('delegates account calls to HTTP services', () => {
    getAccountProfile(settings);
    changePassword({ currentPassword: 'old', newPassword: 'new' }, settings);

    expect(mocks.getAccountProfileRequest).toHaveBeenCalledWith(settings);
    expect(mocks.changePasswordRequest).toHaveBeenCalledWith(
      { currentPassword: 'old', newPassword: 'new' },
      settings,
    );
  });
});
