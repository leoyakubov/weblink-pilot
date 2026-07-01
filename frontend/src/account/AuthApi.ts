import type {
  ApiSettings,
  AccountActionPreviewResponse,
  AuthCredentialsRequest,
  AuthResponse,
  EmailVerificationConfirmRequest,
  EmailVerificationRequest,
  OAuthLoginCompleteRequest,
  PasswordResetConfirmRequest,
  PasswordResetRequest,
  UserProfileResponse,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import {
  completeGithubLoginRequest,
  confirmEmailVerificationRequest,
  confirmPasswordResetRequest,
  getCurrentUserRequest,
  loginRequest,
  logoutSessionRequest,
  refreshTokensRequest,
  registerRequest,
  requestEmailVerificationRequest,
  requestPasswordResetRequest,
} from '@/shared/services/http';

export function login(request: AuthCredentialsRequest, settings: ApiSettings = loadSettings()) {
  return loginRequest(request, settings);
}

export function register(request: AuthCredentialsRequest, settings: ApiSettings = loadSettings()) {
  return registerRequest(request, settings);
}

export function refreshTokens(settings: ApiSettings = loadSettings()) {
  return refreshTokensRequest(settings);
}

export function logoutSession(settings: ApiSettings = loadSettings()) {
  return logoutSessionRequest(settings);
}

export function getCurrentUser(settings: ApiSettings = loadSettings()) {
  return getCurrentUserRequest(settings);
}

export function requestPasswordReset(
  request: PasswordResetRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestPasswordResetRequest(request, settings);
}

export function confirmPasswordReset(
  request: PasswordResetConfirmRequest,
  settings: ApiSettings = loadSettings(),
) {
  return confirmPasswordResetRequest(request, settings);
}

export function requestEmailVerification(
  request: EmailVerificationRequest,
  settings: ApiSettings = loadSettings(),
) {
  return requestEmailVerificationRequest(request, settings);
}

export function confirmEmailVerification(
  request: EmailVerificationConfirmRequest,
  settings: ApiSettings = loadSettings(),
) {
  return confirmEmailVerificationRequest(request, settings);
}

export function completeGithubLogin(
  request: OAuthLoginCompleteRequest,
  settings: ApiSettings = loadSettings(),
) {
  return completeGithubLoginRequest(request, settings);
}

export type {
  AccountActionPreviewResponse,
  AuthCredentialsRequest,
  AuthResponse,
  UserProfileResponse,
};
