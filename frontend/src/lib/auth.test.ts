import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AuthResponse, UserProfileResponse } from '@/types';

const mocks = vi.hoisted(() => ({
  getCurrentUserMock: vi.fn(),
  loginMock: vi.fn(),
  logoutSessionMock: vi.fn(),
  registerMock: vi.fn(),
  refreshTokensMock: vi.fn(),
  loadSettingsMock: vi.fn(),
  saveSettingsMock: vi.fn(),
  hasSessionActiveHintMock: vi.fn(),
  markSessionActiveMock: vi.fn(),
  clearSessionActiveMock: vi.fn(),
}));

const settingsState = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  authToken: '',
  refreshToken: '',
};

vi.mock('@/lib/api', () => ({
  getCurrentUser: mocks.getCurrentUserMock,
  login: mocks.loginMock,
  logoutSession: mocks.logoutSessionMock,
  register: mocks.registerMock,
  refreshTokens: mocks.refreshTokensMock,
}));

vi.mock('@/lib/settings', () => ({
  clearSessionActive: mocks.clearSessionActiveMock,
  hasSessionActiveHint: mocks.hasSessionActiveHintMock,
  loadSettings: mocks.loadSettingsMock,
  markSessionActive: mocks.markSessionActiveMock,
  saveSettings: mocks.saveSettingsMock,
}));

async function loadAuthModule() {
  vi.resetModules();
  return import('./auth');
}

beforeEach(() => {
  vi.clearAllMocks();
  settingsState.apiBaseUrl = 'http://localhost:8080/api/v1';
  settingsState.authToken = '';
  settingsState.refreshToken = '';
  mocks.logoutSessionMock.mockResolvedValue(undefined);
  mocks.hasSessionActiveHintMock.mockReturnValue(false);
  mocks.loadSettingsMock.mockImplementation(() => ({ ...settingsState }));
  mocks.saveSettingsMock.mockImplementation((settings: typeof settingsState) => {
    settingsState.apiBaseUrl = settings.apiBaseUrl;
    settingsState.authToken = settings.authToken;
    settingsState.refreshToken = settings.refreshToken;
  });
});

describe('auth helpers', () => {
  it('bootstraps the current user when a token is present', async () => {
    const profile: UserProfileResponse = { username: 'admin', role: 'ADMIN' };
    settingsState.authToken = 'jwt-token';
    mocks.getCurrentUserMock.mockResolvedValue(profile);

    const auth = await loadAuthModule();
    await auth.bootstrapAuth();

    expect(mocks.getCurrentUserMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: 'jwt-token',
      refreshToken: '',
    });
    expect(auth.authState.currentUser).toEqual(profile);
    expect(auth.authState.ready).toBe(true);
    expect(auth.authState.loading).toBe(false);
  });

  it('authenticates, saves the token, and signs out again', async () => {
    const authResponse: AuthResponse = {
      token: 'new-token',
      username: 'user',
      role: 'USER',
    };
    mocks.loginMock.mockResolvedValue(authResponse);

    const auth = await loadAuthModule();

    await auth.authenticate('login', { username: 'user', password: 'user123' });
    expect(mocks.loginMock).toHaveBeenCalledWith(
      { username: 'user', password: 'user123' },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: '',
        refreshToken: '',
      },
    );
    expect(settingsState.authToken).toBe('new-token');
    expect(settingsState.refreshToken).toBe('');
    expect(auth.authState.currentUser).toEqual({
      username: 'user',
      role: 'USER',
    });
    expect(auth.isAdminUser()).toBe(false);

    auth.signOut();
    expect(mocks.logoutSessionMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: 'new-token',
      refreshToken: '',
    });
    expect(settingsState.authToken).toBe('');
    expect(auth.authState.currentUser).toBeNull();
    expect(auth.authState.sessionNotice).toContain('Signed out');
  });

  it('refreshes the session when no access token is available', async () => {
    const authResponse: AuthResponse = {
      token: 'refreshed-token',
      username: 'user',
      role: 'USER',
    };
    mocks.refreshTokensMock.mockResolvedValue(authResponse);
    mocks.hasSessionActiveHintMock.mockReturnValue(true);

    const auth = await loadAuthModule();

    await auth.bootstrapAuth();

    expect(mocks.refreshTokensMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: '',
      refreshToken: '',
    });
    expect(settingsState.authToken).toBe('refreshed-token');
    expect(auth.authState.currentUser).toEqual({
      username: 'user',
      role: 'USER',
    });
  });

  it('keeps guest mode when no session hint exists', async () => {
    const auth = await loadAuthModule();

    await auth.bootstrapAuth();

    expect(mocks.getCurrentUserMock).not.toHaveBeenCalled();
    expect(mocks.refreshTokensMock).not.toHaveBeenCalled();
    expect(auth.authState.currentUser).toBeNull();
    expect(auth.authState.ready).toBe(true);
  });
});
