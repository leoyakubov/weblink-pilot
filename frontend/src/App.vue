<script setup lang="ts">
import { computed } from 'vue'
import { onMounted } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { authState, bootstrapAuth, isAdminUser, signOut } from '@/lib/auth'

const route = useRoute()

const navItems = computed(() => {
  const items = [
    { label: 'Home', to: '/' },
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'History', to: '/history' },
    { label: 'About', to: '/about' },
  ]

  if (isAdminUser()) {
    items.push({ label: 'Monitoring', to: '/monitoring' })
  }

  return items
})

const authLinks = computed(() => {
  if (authState.currentUser) {
    return []
  }

  return [
    { label: 'Sign in', to: '/auth/signin' },
    { label: 'Sign up', to: '/auth/signup' },
  ]
})

const userChip = computed(() => authState.currentUser
  ? `Logged in as: ${authState.currentUser.username}`
  : '')

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

  return 'Home'
})

const showSectionHeader = computed(() =>
  !['home', 'about', 'signin', 'signup'].includes(String(route.name ?? '')),
)

onMounted(() => {
  bootstrapAuth()
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
            :key="typeof item.to === 'string' ? item.to : item.label"
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
            class="button button-secondary button-small auth-link-button"
          >
            {{ item.label }}
          </RouterLink>
          <span v-if="userChip" class="user-chip">{{ userChip }}</span>
          <button v-if="authState.currentUser" class="button button-secondary button-small" type="button" @click="signOut()">
            Sign out
          </button>
        </div>
      </div>
    </header>

    <main class="shell">
      <section v-if="showSectionHeader" class="section-header">
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
