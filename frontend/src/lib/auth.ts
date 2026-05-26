import { reactive } from 'vue';
import { getCurrentUser, login, logoutSession, register, refreshTokens } from '@/lib/api';
import { loadSettings, saveSettings } from '@/lib/settings';
import type { AuthCredentialsRequest, AuthResponse, UserProfileResponse } from '@/types';

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

export async function bootstrapAuth() {
  if (bootstrapPromise) {
    return bootstrapPromise;
  }

  bootstrapPromise = (async () => {
    authState.loading = true;
    const settings = loadSettings();

    try {
      if (settings.authToken) {
        authState.currentUser = await getCurrentUser(settings);
      } else if (settings.refreshToken) {
        const response = await refreshTokens({ refreshToken: settings.refreshToken }, settings);
        settings.authToken = response.token;
        settings.refreshToken = response.refreshToken;
        saveSettings(settings);
        authState.currentUser = {
          username: response.username,
          role: response.role,
        };
      } else {
        authState.currentUser = null;
      }
    } catch {
      settings.authToken = '';
      settings.refreshToken = '';
      saveSettings(settings);
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
): Promise<AuthResponse> {
  const settings = loadSettings();
  const response =
    mode === 'login' ? await login(request, settings) : await register(request, settings);

  settings.authToken = response.token;
  settings.refreshToken = response.refreshToken;
  saveSettings(settings);
  authState.currentUser = {
    username: response.username,
    role: response.role,
  };
  showSessionNotice(
    mode === 'login'
      ? `Signed in as ${response.username}`
      : `Created ${response.username} and signed in`,
  );

  return response;
}

export function signOut() {
  const settings = loadSettings();
  if (settings.refreshToken) {
    void logoutSession({ refreshToken: settings.refreshToken }, settings).catch(() => undefined);
  }
  settings.authToken = '';
  settings.refreshToken = '';
  saveSettings(settings);
  authState.currentUser = null;
  showSessionNotice('Signed out. Guest mode active.');
}

export function isAdminUser() {
  return authState.currentUser?.role === 'ADMIN';
}
