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
  signOut,
} from '@/features/auth/services/auth.service';
import type { AuthResponse } from '@/shared/types/api';
import { getPrimaryNavigation } from '@/app/navigation';

const route = useRoute();
const router = useRouter();
const menuOpen = ref(false);

type AuthMessage = {
  type?: string;
  response?: AuthResponse;
};

const navItems = computed(() =>
  getPrimaryNavigation(isLoggedIn.value && authState.currentUser?.role === 'ADMIN'),
);
const accountLabel = computed(() => authState.currentUser?.username ?? '');
const isLoggedIn = computed(() => Boolean(authState.currentUser));
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
          class="layout-menu-button"
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

      <nav class="topbar-nav nav" aria-label="Primary">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: route.path === item.to }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="topbar-actions">
        <div class="auth-links">
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signin"
            class="button button-secondary auth-link-button"
          >
            Log in
          </RouterLink>
          <RouterLink v-else to="/account" class="account-pill" aria-label="Signed in account">
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
            class="button button-primary auth-link-button auth-link-button--accent"
          >
            Sign up
          </RouterLink>
          <Button
            v-else
            type="button"
            class="button button-secondary auth-link-button"
            label="Sign out"
            severity="secondary"
            variant="outlined"
            @click="signOut()"
          />
        </div>
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
            :key="item.to"
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
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signin"
            class="button button-secondary drawer-action"
            @click="closeMenu"
          >
            Log in
          </RouterLink>
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signup"
            class="button button-primary drawer-action drawer-action--accent"
            @click="closeMenu"
          >
            Sign up
          </RouterLink>
          <Button
            v-else
            type="button"
            class="button button-secondary drawer-action"
            label="Sign out"
            severity="secondary"
            variant="outlined"
            @click="signOut()"
          />
        </div>
      </div>
    </Drawer>

    <main class="shell">
      <RouterView />
    </main>
  </div>
</template>
