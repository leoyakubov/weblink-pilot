import { reactive } from 'vue';

export type UiMode = 'legacy' | 'sakai';

const STORAGE_KEY = 'weblinkpilot.frontend.ui-mode';

function getDefaultMode(): UiMode {
  return 'sakai';
}

function normalizeUiMode(value: string | null | undefined): UiMode {
  return value === 'legacy' ? 'legacy' : 'sakai';
}

function loadStoredUiMode(): UiMode {
  if (typeof window === 'undefined') {
    return getDefaultMode();
  }

  return normalizeUiMode(window.localStorage.getItem(STORAGE_KEY));
}

function persistUiMode(mode: UiMode) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, mode);
}

function applyUiModeClasses(mode: UiMode) {
  if (typeof document === 'undefined') {
    return;
  }

  const root = document.documentElement;
  root.dataset.uiMode = mode;
  root.classList.toggle('app-dark', mode === 'legacy');
}

export const uiModeState = reactive({
  mode: loadStoredUiMode(),
});

export function initializeUiMode() {
  applyUiModeClasses(uiModeState.mode);
}

export function setUiMode(mode: UiMode) {
  uiModeState.mode = mode;
  persistUiMode(mode);
  applyUiModeClasses(mode);
}

export function toggleUiMode() {
  setUiMode(uiModeState.mode === 'legacy' ? 'sakai' : 'legacy');
}

export function isLegacyUiMode() {
  return uiModeState.mode === 'legacy';
}
