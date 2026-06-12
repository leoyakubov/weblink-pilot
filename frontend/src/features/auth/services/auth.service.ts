import { reactive } from 'vue';
import type { AuthCredentialsRequest, AuthResponse, UserProfileResponse } from '@/shared/types/api';
import {
  clearSessionActive,
  hasSessionActiveHint,
  markSessionActive,
  loadSettings,
  saveSettings,
} from '@/shared/services/settings';
import {
  getCurrentUser,
  login,
  logoutSession,
  refreshTokens,
  register,
} from '@/features/auth/repositories/auth.repository';

export const authState = reactive({
  currentUser: null as UserProfileResponse | null,
  ready: false,
  loading: false,
  sessionNotice: '',
});

let bootstrapPromise: Promise<void> | null = null;
let noticeTimer: ReturnType<typeof globalThis.setTimeout> | null = null;

function showSessionNotice(message: string) {
  authState.sessionNotice = message;
  if (noticeTimer) {
    globalThis.clearTimeout(noticeTimer);
  }
  noticeTimer = globalThis.setTimeout(() => {
    authState.sessionNotice = '';
    noticeTimer = null;
  }, 2400);
}

export function applyAuthResponse(response: AuthResponse, notice?: string) {
  const settings = loadSettings();
  settings.authToken = response.token;
  saveSettings(settings);
  markSessionActive();
  authState.currentUser = {
    username: response.username,
    role: response.role,
  };
  if (notice) {
    showSessionNotice(notice);
  }
}

export async function bootstrapAuth() {
  if (bootstrapPromise) {
    return bootstrapPromise;
  }

  bootstrapPromise = (async () => {
    authState.loading = true;
    const settings = loadSettings();
    const requestSettings = { ...settings };

    try {
      if (settings.authToken) {
        try {
          authState.currentUser = await getCurrentUser(requestSettings);
          markSessionActive();
        } catch {
          if (!hasSessionActiveHint()) {
            settings.authToken = '';
            saveSettings(settings);
            authState.currentUser = null;
            return;
          }

          const response = await refreshTokens(requestSettings);
          applyAuthResponse(response);
        }
      } else if (hasSessionActiveHint()) {
        const response = await refreshTokens(requestSettings);
        applyAuthResponse(response);
      } else {
        authState.currentUser = null;
      }
    } catch {
      settings.authToken = '';
      saveSettings(settings);
      clearSessionActive();
      authState.currentUser = null;
    } finally {
      authState.loading = false;
      authState.ready = true;
    }
  })();

  return bootstrapPromise;
}

export async function authenticate(
  mode: 'login' | 'register',
  request: AuthCredentialsRequest,
): Promise<AuthResponse | void> {
  const settings = loadSettings();
  const requestSettings = { ...settings };
  if (mode === 'login') {
    const response = await login(request, requestSettings);
    applyAuthResponse(response, `Signed in as ${response.username}`);
    return response;
  }

  await register(request, requestSettings);
}

export function signOut() {
  const settings = loadSettings();
  void logoutSession({ ...settings }).catch(() => undefined);
  settings.authToken = '';
  saveSettings(settings);
  clearSessionActive();
  authState.currentUser = null;
  showSessionNotice('Signed out. Guest mode active.');
}

export function isAdminUser() {
  return authState.currentUser?.role === 'ADMIN';
}
