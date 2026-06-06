<script setup lang="ts">
/* global MessageEvent */
import { computed, onBeforeUnmount, onMounted } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import {
  applyAuthResponse,
  authState,
  bootstrapAuth,
  isAdminUser,
  signOut,
} from '@/shared/services/auth';
import type { AuthResponse } from '@/shared/models/api';

const route = useRoute();
const router = useRouter();

type AuthMessage = {
  type?: string;
  response?: AuthResponse;
};

const navItems = computed(() => {
  const items = [
    { label: 'Home', to: '/' },
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'History', to: '/history' },
    { label: 'About', to: '/about' },
  ];

  if (isAdminUser()) {
    items.push({ label: 'Monitoring', to: '/monitoring' });
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
    <div class="glow glow-a"></div>
    <div class="glow glow-b"></div>

    <header class="topbar">
      <RouterLink to="/" class="brand">
        <span class="brand-mark">WP</span>
        <span class="brand-copy">
          <strong>WebLinkPilot</strong>
          <small>Mobile-first short link cockpit</small>
        </span>
      </RouterLink>

      <div class="topbar-actions">
        <nav class="nav">
          <RouterLink
            v-for="item in navItems"
            :key="typeof item.to === 'string' ? item.to : item.label"
            :to="item.to"
            class="nav-link"
            :class="{ active: route.path === item.to }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="auth-links">
          <div class="session-controls">
            <span class="session-slot">
              <RouterLink
                v-if="!isLoggedIn"
                to="/auth/signin"
                class="button button-secondary button-small auth-link-button"
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
            </span>

            <span class="session-slot session-slot--secondary">
              <RouterLink
                v-if="!isLoggedIn"
                to="/auth/signup"
                class="button button-secondary button-small auth-link-button"
              >
                Sign up
              </RouterLink>
              <button
                v-else
                class="button button-secondary button-small"
                type="button"
                @click="signOut()"
              >
                Sign out
              </button>
            </span>
          </div>
        </div>
      </div>
      <Transition name="session-notice">
        <div v-if="authState.sessionNotice" class="session-notice" role="status" aria-live="polite">
          {{ authState.sessionNotice }}
        </div>
      </Transition>
    </header>

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
