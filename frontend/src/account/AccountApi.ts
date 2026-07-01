import type {
  ApiSettings,
  AccountProfileResponse,
  PasswordChangeRequest,
} from '@/shared/types/api';
import { loadSettings } from '@/shared/services/settings';
import { changePasswordRequest, getAccountProfileRequest } from '@/shared/services/http';

export function getAccountProfile(settings: ApiSettings = loadSettings()) {
  return getAccountProfileRequest(settings);
}

export function changePassword(
  request: PasswordChangeRequest,
  settings: ApiSettings = loadSettings(),
) {
  return changePasswordRequest(request, settings);
}

export type { AccountProfileResponse };
