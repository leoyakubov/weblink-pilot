<script setup lang="ts">
/* global MessageEvent */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import Button from 'primevue/button';
import Drawer from 'primevue/drawer';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import {
  applyAuthResponse,
  authState,
  bootstrapAuth,
  isAdminUser,
  signOut,
} from '@/lib/auth';
import type { AuthResponse } from '@/shared/models/api';
import { toggleUiMode, uiModeState } from '@/lib/ui-mode';

const route = useRoute();
const router = useRouter();
const menuOpen = ref(false);

type AuthMessage = {
  type?: string;
  response?: AuthResponse;
};

const navItems = computed(() => {
  const items = [
    { label: 'Home', icon: 'pi pi-home', to: '/' },
    { label: 'Dashboard', icon: 'pi pi-chart-bar', to: '/dashboard' },
    { label: 'History', icon: 'pi pi-history', to: '/history' },
    { label: 'About', icon: 'pi pi-info-circle', to: '/about' },
  ];

  if (isAdminUser()) {
    items.push({ label: 'Monitoring', icon: 'pi pi-shield', to: '/monitoring' });
  }

  return items;
});

const accountLabel = computed(() => authState.currentUser?.username ?? '');
const isLoggedIn = computed(() => Boolean(authState.currentUser));

const currentSection = computed(() => {
  if (route.name === 'link') {
    return 'Link details';
  }

  if (route.name === 'dashboard') {
    return 'Analytics shell';
  }

  if (route.name === 'history') {
    return 'Link history';
  }

  if (route.name === 'monitoring') {
    return 'Admin monitoring';
  }

  if (route.name === 'account') {
    return 'Account settings';
  }

  if (route.name === 'about') {
    return 'About this product';
  }

  return 'Home';
});

const showSectionHeader = computed(
  () =>
    !['home', 'about', 'signin', 'signup', 'github-login-complete', 'account'].includes(
      String(route.name ?? ''),
    ),
);

const themeButtonLabel = computed(() =>
  uiModeState.mode === 'legacy' ? 'Switch to Sakai' : 'Switch to legacy',
);
const themeButtonIcon = computed(() => (uiModeState.mode === 'legacy' ? 'pi pi-sun' : 'pi pi-moon'));

function closeMenu() {
  menuOpen.value = false;
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

onMounted(() => {
  bootstrapAuth();
  window.addEventListener('message', handleAuthMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleAuthMessage);
});
</script>

<template>
  <div class="app-frame">
    <div class="ambient ambient-a"></div>
    <div class="ambient ambient-b"></div>

    <header class="topbar">
      <div class="topbar-brand">
        <Button
          type="button"
          class="layout-topbar-action layout-menu-button"
          icon="pi pi-bars"
          severity="secondary"
          variant="text"
          rounded
          aria-label="Open navigation"
          @click="menuOpen = true"
        />
        <RouterLink to="/" class="brand">
          <span class="brand-mark">WP</span>
          <span class="brand-copy">
            <strong>WebLinkPilot</strong>
            <small>Mobile-first short link cockpit</small>
          </span>
        </RouterLink>
      </div>

      <div class="topbar-actions">
        <Button
          type="button"
          class="layout-topbar-action layout-topbar-action-highlight"
          :icon="themeButtonIcon"
          severity="secondary"
          variant="text"
          rounded
          :aria-label="themeButtonLabel"
          @click="toggleUiMode"
        />

        <RouterLink
          v-if="!isLoggedIn"
          to="/auth/signin"
          class="layout-topbar-action layout-topbar-link"
        >
          Log in
        </RouterLink>
        <RouterLink v-else to="/account" class="layout-topbar-account" aria-label="Signed in account">
          <span class="account-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" role="presentation" focusable="false">
              <path
                d="M12 12.5c2.9 0 5.25-2.46 5.25-5.5S14.9 1.5 12 1.5 6.75 3.96 6.75 7s2.35 5.5 5.25 5.5Zm0 2.25c-4.36 0-7.95 2.75-8.45 6.25h16.9c-.5-3.5-4.09-6.25-8.45-6.25Z"
              />
            </svg>
          </span>
          <span class="account-name">{{ accountLabel }}</span>
        </RouterLink>

        <RouterLink
          v-if="!isLoggedIn"
          to="/auth/signup"
          class="layout-topbar-action layout-topbar-link layout-topbar-link--accent"
        >
          Sign up
        </RouterLink>
        <Button
          v-else
          type="button"
          class="layout-topbar-action"
          label="Sign out"
          severity="secondary"
          variant="outlined"
          @click="signOut()"
        />
      </div>

      <Transition name="session-notice">
        <div v-if="authState.sessionNotice" class="session-notice" role="status" aria-live="polite">
          {{ authState.sessionNotice }}
        </div>
      </Transition>
    </header>

    <Drawer
      v-model:visible="menuOpen"
      position="left"
      :show-close-icon="false"
      class="app-drawer"
      modal
      @hide="closeMenu"
    >
      <div class="drawer-panel">
        <div class="drawer-header">
          <RouterLink to="/" class="brand" @click="closeMenu">
            <span class="brand-mark">WP</span>
            <span class="brand-copy">
              <strong>WebLinkPilot</strong>
              <small>Mobile-first short link cockpit</small>
            </span>
          </RouterLink>

          <Button
            type="button"
            icon="pi pi-times"
            severity="secondary"
            variant="text"
            rounded
            aria-label="Close navigation"
            @click="closeMenu"
          />
        </div>

        <nav class="drawer-nav" aria-label="Primary">
          <RouterLink
            v-for="item in navItems"
            :key="typeof item.to === 'string' ? item.to : item.label"
            :to="item.to"
            class="drawer-nav-item"
            :class="{ active: route.path === item.to }"
            @click="closeMenu"
          >
            <i :class="item.icon" aria-hidden="true"></i>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="drawer-actions">
          <Button
            type="button"
            class="drawer-action"
            :label="themeButtonLabel"
            :icon="themeButtonIcon"
            severity="secondary"
            variant="outlined"
            @click="toggleUiMode"
          />
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signin"
            class="drawer-action drawer-action--link"
            @click="closeMenu"
          >
            Log in
          </RouterLink>
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signup"
            class="drawer-action drawer-action--link drawer-action--accent"
            @click="closeMenu"
          >
            Sign up
          </RouterLink>
          <Button
            v-else
            type="button"
            class="drawer-action"
            label="Sign out"
            severity="secondary"
            variant="outlined"
            @click="signOut()"
          />
        </div>
      </div>
    </Drawer>

    <main class="shell">
      <section v-if="showSectionHeader" class="section-header">
        <p class="eyebrow">Frontend foundation</p>
        <h1>{{ currentSection }}</h1>
        <p class="lede">
          Vue 3 app wired for short links, QR previews, and future analytics. Built to feel good on
          a phone first.
        </p>
      </section>

      <RouterView />
    </main>
  </div>
</template>
