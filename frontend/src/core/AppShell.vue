<script setup lang="ts">
import Button from 'primevue/button';
import Drawer from 'primevue/drawer';
import { RouterLink, RouterView } from 'vue-router';
import { useAppShell } from './AppShell';
import './AppShell.css';

const {
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
} = useAppShell();
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
          rounded
          aria-label="Open navigation"
          @click="menuOpen = true"
        />
        <RouterLink to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">
            <img src="/content/images/weblinkpilot-icon-512.png" alt="" />
          </span>
          <span class="brand-copy">
            <strong>WeblinkPilot</strong>
            <small>Personal short link workspace</small>
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
          <i :class="item.icon" aria-hidden="true"></i>
          <span>{{ item.label }}</span>
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
          <div v-else class="account-menu">
            <button
              type="button"
              class="account-pill"
              aria-label="Open account menu"
              :aria-expanded="accountMenuOpen"
              @click.stop="accountMenuOpen = !accountMenuOpen"
            >
              <span class="account-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" role="presentation" focusable="false">
                  <path
                    d="M12 12.5c2.9 0 5.25-2.46 5.25-5.5S14.9 1.5 12 1.5 6.75 3.96 6.75 7s2.35 5.5 5.25 5.5Zm0 2.25c-4.36 0-7.95 2.75-8.45 6.25h16.9c-.5-3.5-4.09-6.25-8.45-6.25Z"
                  />
                </svg>
              </span>
              <span class="account-name">{{ accountLabel }}</span>
              <i class="pi pi-angle-down account-menu__chevron" aria-hidden="true"></i>
            </button>

            <div v-if="accountMenuOpen" class="account-menu__panel">
              <RouterLink to="/account" class="account-menu__item" @click="closeAccountMenu">
                <i class="pi pi-user" aria-hidden="true"></i>
                <span>Account</span>
              </RouterLink>
              <RouterLink
                v-if="isAdmin"
                to="/admin/users"
                class="account-menu__item"
                @click="closeAccountMenu"
              >
                <i class="pi pi-users" aria-hidden="true"></i>
                <span>Users</span>
              </RouterLink>
              <RouterLink
                v-if="isAdmin"
                to="/monitoring"
                class="account-menu__item"
                @click="closeAccountMenu"
              >
                <i class="pi pi-chart-bar" aria-hidden="true"></i>
                <span>Monitoring</span>
              </RouterLink>
              <button type="button" class="account-menu__item" @click="handleSignOut">
                <i class="pi pi-sign-out" aria-hidden="true"></i>
                <span>Sign out</span>
              </button>
            </div>
          </div>

          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signup"
            class="button button-primary auth-link-button auth-link-button--accent"
          >
            Sign up
          </RouterLink>
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
            <span class="brand-mark" aria-hidden="true">
              <img src="/content/images/weblinkpilot-icon-512.png" alt="" />
            </span>
            <span class="brand-copy">
              <strong>WeblinkPilot</strong>
              <small>Personal short link workspace</small>
            </span>
          </RouterLink>

          <Button
            type="button"
            class="drawer-close-button"
            icon="pi pi-times"
            severity="secondary"
            variant="text"
            rounded
            aria-label="Close navigation"
            @click="closeMenu"
          />
        </div>

        <div class="drawer-session">
          <p class="eyebrow">Workspace</p>
          <strong>{{ isLoggedIn ? accountLabel : 'Guest mode' }}</strong>
          <span>{{
            isLoggedIn
              ? 'Private links, saved history, and analytics are available.'
              : 'Create demo links now, or sign in to keep history.'
          }}</span>
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
            <i class="pi pi-sign-in" aria-hidden="true"></i>
            <span>Log in</span>
          </RouterLink>
          <RouterLink
            v-if="!isLoggedIn"
            to="/auth/signup"
            class="button button-primary drawer-action drawer-action--accent"
            @click="closeMenu"
          >
            <i class="pi pi-user-plus" aria-hidden="true"></i>
            <span>Sign up</span>
          </RouterLink>
          <RouterLink
            v-if="isAdmin"
            to="/monitoring"
            class="button button-secondary drawer-action"
            @click="closeMenu"
          >
            <i class="pi pi-chart-bar" aria-hidden="true"></i>
            <span>Monitoring</span>
          </RouterLink>
          <Button
            v-if="isLoggedIn"
            type="button"
            class="button button-secondary drawer-action sign-out-button"
            label="Sign out"
            icon="pi pi-sign-out"
            severity="secondary"
            variant="outlined"
            @click="handleSignOut"
          />
        </div>
      </div>
    </Drawer>

    <main class="shell">
      <RouterView />
    </main>

    <footer class="page-footer app-footer">
      <div class="page-footer__brand-block">
        <RouterLink to="/" class="brand page-footer__brand-link">
          <span class="brand-mark" aria-hidden="true">
            <img src="/content/images/weblinkpilot-icon-512.png" alt="" />
          </span>
          <span class="brand-copy">
            <strong>WeblinkPilot</strong>
            <small>Personal short link workspace</small>
          </span>
        </RouterLink>
        <p>Clean short links, QR codes, and click history for everyday sharing.</p>
        <p class="page-footer__copyright">
          © 2026 WeblinkPilot. Built for fast personal link sharing.
        </p>
      </div>

      <div class="page-footer__section">
        <p class="page-footer__heading">Navigate</p>
        <div class="page-footer__links">
          <RouterLink to="/">Home</RouterLink>
          <RouterLink to="/links">Links</RouterLink>
          <RouterLink to="/analytics">Analytics</RouterLink>
          <RouterLink to="/about">About</RouterLink>
        </div>
      </div>

      <div class="page-footer__section">
        <p class="page-footer__heading">Project</p>
        <div class="page-footer__links">
          <a href="https://github.com/leoyakubov/weblink-pilot" target="_blank" rel="noreferrer">
            GitHub
          </a>
          <a
            href="https://github.com/leoyakubov/weblink-pilot/blob/main/docs/README.md"
            target="_blank"
            rel="noreferrer"
          >
            Docs
          </a>
          <a
            href="https://github.com/leoyakubov/weblink-pilot/blob/main/docs/planning/roadmap.md"
            target="_blank"
            rel="noreferrer"
          >
            Roadmap
          </a>
        </div>
      </div>
    </footer>
  </div>
</template>
