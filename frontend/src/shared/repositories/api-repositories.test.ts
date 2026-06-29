import { describe, expect, it, vi } from 'vitest';
import {
  getAccountProfile,
  changePassword,
} from '@/features/account/repositories/account.repository';
import { listAdminUsers } from '@/features/admin/repositories/admin.repository';
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
} from '@/features/auth/repositories/auth.repository';
import {
  createLink,
  getAiLinkMetadata,
  getAnalyticsDetails,
  getAnalyticsSummary,
  getLink,
  getLinkCreatorOptions,
  getRedirectPreview,
  listLinks,
  regenerateAiLinkMetadata,
} from '@/features/links/repositories/link.repository';
import {
  getAdminMonitoring,
  getAdminOverview,
} from '@/features/monitoring/repositories/monitoring.repository';
import type { ApiSettings } from '@/shared/types/api';

const mocks = vi.hoisted(() => ({
  calls: new Map<string, ReturnType<typeof vi.fn>>(),
  settings: {
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: 'token',
    refreshToken: 'refresh',
  },
  httpMock(name: string) {
    const mock = vi.fn((...args: unknown[]) => ({ name, args }));
    this.calls.set(name, mock);
    return mock;
  },
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => mocks.settings,
}));

vi.mock('@/shared/services/http', () => ({
  changePasswordRequest: mocks.httpMock('changePasswordRequest'),
  completeGithubLoginRequest: mocks.httpMock('completeGithubLoginRequest'),
  confirmEmailVerificationRequest: mocks.httpMock('confirmEmailVerificationRequest'),
  confirmPasswordResetRequest: mocks.httpMock('confirmPasswordResetRequest'),
  createLinkRequest: mocks.httpMock('createLinkRequest'),
  getAccountProfileRequest: mocks.httpMock('getAccountProfileRequest'),
  getAdminMonitoringRequest: mocks.httpMock('getAdminMonitoringRequest'),
  getAdminOverviewRequest: mocks.httpMock('getAdminOverviewRequest'),
  getAiLinkMetadataRequest: mocks.httpMock('getAiLinkMetadataRequest'),
  getAnalyticsDetailsRequest: mocks.httpMock('getAnalyticsDetailsRequest'),
  getAnalyticsSummaryRequest: mocks.httpMock('getAnalyticsSummaryRequest'),
  getCurrentUserRequest: mocks.httpMock('getCurrentUserRequest'),
  getLinkCreatorOptionsRequest: mocks.httpMock('getLinkCreatorOptionsRequest'),
  getLinkRequest: mocks.httpMock('getLinkRequest'),
  getRedirectPreviewRequest: mocks.httpMock('getRedirectPreviewRequest'),
  listAdminUsersRequest: mocks.httpMock('listAdminUsersRequest'),
  listLinksRequest: mocks.httpMock('listLinksRequest'),
  loginRequest: mocks.httpMock('loginRequest'),
  logoutSessionRequest: mocks.httpMock('logoutSessionRequest'),
  refreshTokensRequest: mocks.httpMock('refreshTokensRequest'),
  regenerateAiLinkMetadataRequest: mocks.httpMock('regenerateAiLinkMetadataRequest'),
  registerRequest: mocks.httpMock('registerRequest'),
  requestEmailVerificationRequest: mocks.httpMock('requestEmailVerificationRequest'),
  requestPasswordResetRequest: mocks.httpMock('requestPasswordResetRequest'),
}));

const settings: ApiSettings = {
  apiBaseUrl: 'http://api.test/api/v1',
  authToken: 'jwt',
  refreshToken: 'refresh',
};

describe('API repositories', () => {
  it('delegates auth repository calls to HTTP services', () => {
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
    expect(mocks.calls.get('completeGithubLoginRequest')).toHaveBeenCalledWith(
      { ticket: 'ticket' },
      settings,
    );
  });

  it('delegates account, admin, monitoring, and link repository calls', () => {
    getAccountProfile(settings);
    changePassword({ currentPassword: 'old', newPassword: 'new' }, settings);
    listAdminUsers(settings);
    getAdminOverview(settings);
    getAdminMonitoring(settings);
    createLink({ originalUrl: 'https://example.com', customAlias: 'example' }, settings);
    listLinks(10, settings, 'admin', 'ADMIN', 'active');
    getLink('redis', settings);
    getAiLinkMetadata('redis', settings);
    regenerateAiLinkMetadata('redis', settings);
    getRedirectPreview('redis', settings);
    getAnalyticsSummary('redis', settings);
    getAnalyticsDetails('redis', settings);
    getLinkCreatorOptions(settings);

    expect(mocks.calls.get('getAccountProfileRequest')).toHaveBeenCalledWith(settings);
    expect(mocks.calls.get('changePasswordRequest')).toHaveBeenCalledWith(
      { currentPassword: 'old', newPassword: 'new' },
      settings,
    );
    expect(mocks.calls.get('listLinksRequest')).toHaveBeenCalledWith(
      10,
      settings,
      'admin',
      'ADMIN',
      'active',
    );
    expect(mocks.calls.get('getAnalyticsDetailsRequest')).toHaveBeenCalledWith('redis', settings);
  });
});
