import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearSettings,
  defaultSettings,
  hasSessionActiveHint,
  loadSettings,
  normalizeBaseUrl,
  saveSettings,
} from '@/shared/services/settings';

describe('normalizeBaseUrl', () => {
  it('trims whitespace and trailing slashes', () => {
    expect(normalizeBaseUrl('  http://localhost:8080/api/v1///  ')).toBe(
      'http://localhost:8080/api/v1',
    );
  });
});

describe('settings storage', () => {
  let localStorageMock: Storage;
  let sessionStorageMock: Storage;
  let localStorageSpy: ReturnType<typeof vi.spyOn>;
  let sessionStorageSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    const createStorage = (): Storage => {
      const entries = new Map<string, string>();

      return {
        get length() {
          return entries.size;
        },
        clear() {
          entries.clear();
        },
        getItem(key: string) {
          return entries.has(key) ? entries.get(key)! : null;
        },
        key(index: number) {
          return Array.from(entries.keys())[index] ?? null;
        },
        removeItem(key: string) {
          entries.delete(key);
        },
        setItem(key: string, value: string) {
          entries.set(key, value);
        },
      } as Storage;
    };

    localStorageMock = createStorage();
    sessionStorageMock = createStorage();
    localStorageSpy = vi.spyOn(window, 'localStorage', 'get').mockReturnValue(localStorageMock);
    sessionStorageSpy = vi
      .spyOn(window, 'sessionStorage', 'get')
      .mockReturnValue(sessionStorageMock);
  });

  afterEach(() => {
    localStorageSpy.mockRestore();
    sessionStorageSpy.mockRestore();
    vi.restoreAllMocks();
  });

  it('returns defaults when storage is empty', () => {
    expect(loadSettings()).toEqual(defaultSettings());
  });

  it('round-trips saved values', () => {
    saveSettings({
      apiBaseUrl: 'http://localhost:8080/api/v1/',
      authToken: 'jwt-token',
      refreshToken: 'refresh-token',
    });

    expect(loadSettings()).toEqual({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: 'jwt-token',
      refreshToken: '',
    });
  });

  it('falls back to defaults when stored JSON is invalid', () => {
    window.localStorage.setItem('weblinkpilot.frontend.settings', 'not-json');

    expect(loadSettings()).toEqual(defaultSettings());
  });

  it('clears saved browser state', () => {
    saveSettings({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: 'jwt-token',
      refreshToken: 'refresh-token',
    });
    window.sessionStorage.setItem('weblinkpilot.frontend.session.active', '1');

    clearSettings();

    expect(window.localStorage.getItem('weblinkpilot.frontend.settings')).toBeNull();
    expect(window.sessionStorage.getItem('weblinkpilot.frontend.session')).toBeNull();
    expect(window.sessionStorage.getItem('weblinkpilot.frontend.session.active')).toBeNull();
  });

  it('falls back when browser storage is unavailable', () => {
    localStorageSpy.mockRestore();
    sessionStorageSpy.mockRestore();

    vi.spyOn(window, 'localStorage', 'get').mockImplementation(() => {
      throw new DOMException('Cannot initialize local storage', 'SecurityError');
    });
    vi.spyOn(window, 'sessionStorage', 'get').mockImplementation(() => {
      throw new DOMException('Cannot initialize session storage', 'SecurityError');
    });

    expect(loadSettings()).toEqual(defaultSettings());

    expect(() =>
      saveSettings({
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: 'jwt-token',
        refreshToken: 'refresh-token',
      }),
    ).not.toThrow();
    expect(() => clearSettings()).not.toThrow();
    expect(hasSessionActiveHint()).toBe(false);
  });
});
