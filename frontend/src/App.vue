<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { label: 'Create', to: '/' },
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'History', to: '/history' },
  { label: 'Monitoring', to: '/monitoring' },
  { label: 'About', to: '/about' },
]

const authLinks = [
  { label: 'Sign in', to: { path: '/', hash: '#access', query: { auth: 'login' } } },
  { label: 'Sign up', to: { path: '/', hash: '#access', query: { auth: 'register' } } },
]

const currentSection = computed(() => {
  if (route.name === 'link') {
    return 'Link details'
  }

  if (route.name === 'dashboard') {
    return 'Analytics shell'
  }

  if (route.name === 'history') {
    return 'Link history'
  }

  if (route.name === 'monitoring') {
    return 'Admin monitoring'
  }

  if (route.name === 'about') {
    return 'About this product'
  }

  return 'Create link'
})
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
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: route.path === item.to }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="auth-links">
          <RouterLink
            v-for="item in authLinks"
            :key="String(item.label)"
            :to="item.to"
            class="button button-secondary button-small"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="shell">
      <section class="section-header">
        <p class="eyebrow">Frontend foundation</p>
        <h1>{{ currentSection }}</h1>
        <p class="lede">
          Vue 3 app wired for short links, QR previews, and future analytics. Built to feel good on a phone first.
        </p>
      </section>

      <RouterView />
    </main>
  </div>
</template>
