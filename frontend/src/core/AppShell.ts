/* global MessageEvent, MouseEvent */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { applyAuthResponse, authState, bootstrapAuth, signOut } from '@/account/AuthSession';
import type { AuthResponse } from '@/shared/types/api';
import { getPrimaryNavigation } from './AppShellNavigation';

type AuthMessage = {
  type?: string;
  response?: AuthResponse;
};

export function useAppShell() {
  const route = useRoute();
  const router = useRouter();
  const menuOpen = ref(false);
  const accountMenuOpen = ref(false);

  const navItems = computed(() => getPrimaryNavigation());
  const accountLabel = computed(() => authState.currentUser?.username ?? '');
  const isLoggedIn = computed(() => Boolean(authState.currentUser));
  const isAdmin = computed(() => isLoggedIn.value && authState.currentUser?.role === 'ADMIN');

  function closeMenu() {
    menuOpen.value = false;
  }

  function closeAccountMenu() {
    accountMenuOpen.value = false;
  }

  function handleSignOut() {
    signOut();
    closeMenu();
    closeAccountMenu();
    void router.push('/');
  }

  function handleAuthMessage(event: MessageEvent) {
    if (event.origin !== window.location.origin) {
      return;
    }

    const message = event.data as AuthMessage | undefined;
    if (message?.type !== 'weblinkpilot:github-login' || !message.response?.token) {
      return;
    }

    applyAuthResponse(message.response, `Signed in as ${message.response.username}`);
    void router.push('/');
  }

  function closeAccountMenuOnOutsideClick(event: MouseEvent) {
    const target = event.target as { closest?: (selector: string) => unknown } | null;

    if (target?.closest?.('.account-menu')) {
      return;
    }

    closeAccountMenu();
  }

  onMounted(() => {
    bootstrapAuth();
    window.addEventListener('message', handleAuthMessage);
    document.addEventListener('click', closeAccountMenuOnOutsideClick);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('message', handleAuthMessage);
    document.removeEventListener('click', closeAccountMenuOnOutsideClick);
  });

  return {
    accountLabel,
    accountMenuOpen,
    authState,
    closeAccountMenu,
    closeMenu,
    handleSignOut,
    isAdmin,
    isLoggedIn,
    menuOpen,
    navItems,
    route,
  };
}
