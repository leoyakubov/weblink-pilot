import { beforeEach, describe, expect, it } from 'vitest';
import {
  defaultSettings,
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
  beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
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
});
