import { describe, expect, it, vi } from 'vitest';
import {
  completeGithubLogin,
  confirmEmailVerification,
  confirmPasswordReset,
  getCurrentUser,
  login,
  logoutSession,
  refreshTokens,
  register,
  requestEmailVerification,
  requestPasswordReset,
} from './AuthApi';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  calls: new Map<string, ReturnType<typeof vi.fn>>(),
  httpMock(name: string) {
    const mock = vi.fn((...args: unknown[]) => ({ name, args }));
    this.calls.set(name, mock);
    return mock;
  },
}));

vi.mock('@/shared/services/http', () => ({
  completeGithubLoginRequest: mocks.httpMock('completeGithubLoginRequest'),
  confirmEmailVerificationRequest: mocks.httpMock('confirmEmailVerificationRequest'),
  confirmPasswordResetRequest: mocks.httpMock('confirmPasswordResetRequest'),
  getCurrentUserRequest: mocks.httpMock('getCurrentUserRequest'),
  loginRequest: mocks.httpMock('loginRequest'),
  logoutSessionRequest: mocks.httpMock('logoutSessionRequest'),
  refreshTokensRequest: mocks.httpMock('refreshTokensRequest'),
  registerRequest: mocks.httpMock('registerRequest'),
  requestEmailVerificationRequest: mocks.httpMock('requestEmailVerificationRequest'),
  requestPasswordResetRequest: mocks.httpMock('requestPasswordResetRequest'),
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('AuthApi', () => {
  it('delegates auth calls to HTTP services', () => {
    login({ username: 'user', password: 'pass' }, settings);
    register({ username: 'new-user', password: 'pass' }, settings);
    refreshTokens(settings);
    logoutSession(settings);
    getCurrentUser(settings);
    requestPasswordReset({ email: 'user@example.com' }, settings);
    confirmPasswordReset({ token: 'reset-token', password: 'new-pass' }, settings);
    requestEmailVerification({ email: 'user@example.com' }, settings);
    confirmEmailVerification({ token: 'verify-token' }, settings);
    completeGithubLogin({ ticket: 'ticket' }, settings);

    expect(mocks.calls.get('loginRequest')).toHaveBeenCalledWith(
      { username: 'user', password: 'pass' },
      settings,
    );
    expect(mocks.calls.get('registerRequest')).toHaveBeenCalledWith(
      { username: 'new-user', password: 'pass' },
      settings,
    );
    expect(mocks.calls.get('refreshTokensRequest')).toHaveBeenCalledWith(settings);
    expect(mocks.calls.get('logoutSessionRequest')).toHaveBeenCalledWith(settings);
    expect(mocks.calls.get('getCurrentUserRequest')).toHaveBeenCalledWith(settings);
    expect(mocks.calls.get('requestPasswordResetRequest')).toHaveBeenCalledWith(
      { email: 'user@example.com' },
      settings,
    );
    expect(mocks.calls.get('confirmPasswordResetRequest')).toHaveBeenCalledWith(
      { token: 'reset-token', password: 'new-pass' },
      settings,
    );
    expect(mocks.calls.get('requestEmailVerificationRequest')).toHaveBeenCalledWith(
      { email: 'user@example.com' },
      settings,
    );
    expect(mocks.calls.get('confirmEmailVerificationRequest')).toHaveBeenCalledWith(
      { token: 'verify-token' },
      settings,
    );
    expect(mocks.calls.get('completeGithubLoginRequest')).toHaveBeenCalledWith(
      { ticket: 'ticket' },
      settings,
    );
  });
});
