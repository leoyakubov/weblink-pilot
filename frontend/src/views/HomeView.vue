<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import CopyActionButton from '@/components/CopyActionButton.vue'
import { buildApiBaseUrl, createLink, getCurrentUser, login, register } from '@/lib/api'
import { loadSettings, saveSettings } from '@/lib/settings'
import type { ApiSettings, AuthCredentialsRequest, CreateLinkRequest, LinkResponse, UserProfileResponse } from '@/types'

const route = useRoute()
const settings = reactive<ApiSettings>(loadSettings())
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://example.com/docs/getting-started',
  customAlias: '',
  expiresAt: '',
})
const authForm = reactive<AuthCredentialsRequest>({
  username: '',
  password: '',
})

const createdLink = ref<LinkResponse | null>(null)
const currentUser = ref<UserProfileResponse | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const authError = ref('')
const authSuccess = ref('')
const submitting = ref(false)
const authBusy = ref(false)
const authMode = ref<'login' | 'register'>('login')

const connectionStatus = computed(() => {
  if (currentUser.value) {
    return `Signed in as ${currentUser.value.username} (${currentUser.value.role})`
  }
  return 'Guest mode ready for demo links'
})

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
)

const dashboardUrl = computed(() =>
  createdLink.value ? { name: 'dashboard', query: { code: createdLink.value.code } } : '/',
)

function syncSettings() {
  saveSettings(settings)
}

async function refreshSession() {
  if (!settings.authToken) {
    currentUser.value = null
    return
  }

  try {
    currentUser.value = await getCurrentUser(settings)
  } catch {
    settings.authToken = ''
    saveSettings(settings)
    currentUser.value = null
  }
}

async function authenticate(mode: 'login' | 'register') {
  authBusy.value = true
  authError.value = ''
  authSuccess.value = ''

  try {
    const response = mode === 'login'
      ? await login(authForm, settings)
      : await register(authForm, settings)

    settings.authToken = response.token
    saveSettings(settings)
    currentUser.value = {
      username: response.username,
      role: response.role,
    }
    authSuccess.value = mode === 'login'
      ? `Signed in as ${response.username}`
      : `Registered ${response.username} and signed in`
    authMode.value = mode
  } catch (error) {
    authError.value = error instanceof Error ? error.message : 'Authentication failed'
  } finally {
    authBusy.value = false
  }
}

function signOut() {
  settings.authToken = ''
  saveSettings(settings)
  currentUser.value = null
  authSuccess.value = 'Signed out'
}

async function submit() {
  errorMessage.value = ''
  successMessage.value = ''
  submitting.value = true

  try {
    syncSettings()

    const originalUrl = form.originalUrl.trim()
    new URL(originalUrl)

    const payload: CreateLinkRequest = {
      originalUrl,
      customAlias: form.customAlias?.trim() || undefined,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
    }

    createdLink.value = await createLink(payload, settings)
    successMessage.value = `Created ${createdLink.value.code} successfully`
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Something went wrong'
  } finally {
    submitting.value = false
  }
}

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  if (route.query.auth === 'register') {
    authMode.value = 'register'
  }

  refreshSession()
})

watch(
  () => route.query.auth,
  value => {
    if (value === 'register' || value === 'login') {
      authMode.value = value
    }
  },
)
</script>

<template>
  <section class="landing-layout stack">
    <article class="card hero-card">
      <div class="card-inner hero-grid">
        <div class="hero-copy">
          <p class="eyebrow">Link management</p>
          <h2 class="hero-title">Short links that feel like a modern SaaS product.</h2>
          <p class="hero-note">
            Create a demo link in seconds, or sign in to keep ownership and private history. The redirect, QR, and
            analytics paths stay public and fast either way.
          </p>

          <div class="hero-badges">
            <span class="badge"><strong>Guest</strong> demo links</span>
            <span class="badge"><strong>Owned</strong> user links</span>
            <span class="badge"><strong>QR</strong> and analytics</span>
          </div>

          <div class="hero-points">
            <div class="hero-point">
              <strong>Anonymous mode</strong>
              <p>Use the app instantly without creating an account.</p>
            </div>
            <div class="hero-point">
              <strong>Signed-in mode</strong>
              <p>Keep private history, ownership, and admin access where appropriate.</p>
            </div>
          </div>
        </div>

        <div class="stack">
          <div>
            <p class="eyebrow">Create short link</p>
            <h3 class="panel-title">Launch a new link</h3>
            <p class="help-text">
              Anonymous demo links still work. If you are signed in, the new link belongs to your account.
            </p>
          </div>

          <form class="form-grid" @submit.prevent="submit">
            <label class="form-field">
              <span class="field-label">Original URL</span>
              <input
                v-model="form.originalUrl"
                class="input"
                type="url"
                placeholder="https://example.com/docs/getting-started"
                required
              />
            </label>

            <label class="form-field">
              <span class="field-label">Custom alias (optional)</span>
              <input v-model="form.customAlias" class="input" type="text" placeholder="github-org" />
              <p class="help-text">Leave this blank to generate a random short code.</p>
            </label>

            <label class="form-field">
              <span class="field-label">Expiration</span>
              <input v-model="form.expiresAt" class="input" type="datetime-local" />
            </label>

            <div class="actions">
              <button class="button button-primary" type="submit" :disabled="submitting">
                {{ submitting ? 'Creating...' : 'Create link' }}
              </button>
              <RouterLink class="button button-secondary" to="/dashboard">
                Open dashboard
              </RouterLink>
            </div>

            <p v-if="errorMessage" class="status error">
              <span class="status-dot"></span>
              {{ errorMessage }}
            </p>
            <p v-else-if="successMessage" class="status">
              <span class="status-dot"></span>
              {{ successMessage }}
            </p>
            <p v-else class="status warning">
              <span class="status-dot"></span>
              {{ connectionStatus }}
            </p>
          </form>
        </div>
      </div>
    </article>

    <article class="card" id="access">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Account access</p>
          <h3 class="panel-title">Sign in or create an account</h3>
          <p class="help-text">
            Use the top-right buttons to jump here. Sign in to own new links and keep private history.
          </p>
        </div>

        <div class="segmented-control" role="tablist" aria-label="Authentication mode">
          <button
            class="segmented-control__button"
            :class="{ active: authMode === 'login' }"
            type="button"
            @click="authMode = 'login'"
          >
            Sign in
          </button>
          <button
            class="segmented-control__button"
            :class="{ active: authMode === 'register' }"
            type="button"
            @click="authMode = 'register'"
          >
            Sign up
          </button>
        </div>

        <div class="grid-2">
          <label class="form-field">
            <span class="field-label">Username</span>
            <input v-model="authForm.username" class="input" type="text" placeholder="admin" />
          </label>
          <label class="form-field">
            <span class="field-label">Password</span>
            <input v-model="authForm.password" class="input" type="password" placeholder="admin123" />
          </label>
        </div>

        <div class="actions">
          <button class="button button-primary" type="button" :disabled="authBusy" @click="authenticate(authMode)">
            {{ authBusy ? 'Working...' : authMode === 'login' ? 'Sign in' : 'Sign up' }}
          </button>
          <button class="button button-secondary" type="button" :disabled="authBusy" @click="authenticate(authMode === 'login' ? 'register' : 'login')">
            {{ authMode === 'login' ? 'Switch to sign up' : 'Switch to sign in' }}
          </button>
          <button class="button button-secondary" type="button" @click="signOut">
            Sign out
          </button>
        </div>

        <p v-if="authError" class="status error">
          <span class="status-dot"></span>
          {{ authError }}
        </p>
        <p v-else-if="authSuccess" class="status">
          <span class="status-dot"></span>
          {{ authSuccess }}
        </p>
        <p v-else class="help-text">
          Guests can create anonymous demo links. Signing in makes new links owned by your account and unlocks private history and analytics.
        </p>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Created link</p>
          <h3 class="panel-title">Share card</h3>
        </div>

        <div v-if="createdLink" class="link-preview">
          <div class="list-item">
            <strong>{{ createdLink.shortUrl }}</strong>
            <p>Short URL ready to copy, open, or share.</p>
          </div>
          <div class="list-item">
            <strong>{{ createdLink.ownerUsername ?? 'Anonymous demo' }}</strong>
            <p>Ownership for this link.</p>
          </div>
          <div class="list-item">
            <strong>{{ createdLink.originalUrl }}</strong>
            <p>Target URL that the redirect endpoint will resolve.</p>
          </div>
          <div class="actions">
            <RouterLink
              class="button button-primary"
              :to="{ name: 'link', params: { code: createdLink.code } }"
            >
              View details page
            </RouterLink>
            <RouterLink class="button button-secondary" :to="dashboardUrl">
              Open analytics
            </RouterLink>
            <CopyActionButton
              :value="createdLink.shortUrl"
              label="Copy short URL"
              copied-label="Short URL copied"
              variant="primary"
            />
            <button class="button button-secondary" type="button" @click="openExternal(createdLink.shortUrl)">
              Open redirect
            </button>
            <CopyActionButton
              :value="linkPreviewUrl"
              label="Copy preview URL"
              copied-label="Preview URL copied"
            />
          </div>
        </div>

        <p v-else class="help-text">
          Create a link to see the share card, QR, and quick actions here.
        </p>
      </div>
    </article>
  </section>

  <section v-if="createdLink" class="page-grid two-col" style="margin-top: 1rem;">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">QR output</p>
          <h3 class="panel-title">Mobile scan ready</h3>
        </div>

        <figure>
          <img class="qr-image" :src="createdLink.qrCodeUrl" :alt="`QR code for ${createdLink.code}`" />
        </figure>

        <div class="grid-2">
          <CopyActionButton
            :value="createdLink.qrCodeUrl"
            label="Copy QR URL"
            copied-label="QR URL copied"
            variant="primary"
          />
          <button class="button button-secondary" type="button" @click="openExternal(createdLink.qrCodeUrl)">
            Open QR image
          </button>
        </div>

        <p class="help-text">
          QR code endpoint: <span class="inline-code">{{ createdLink.qrCodeUrl }}</span>
        </p>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Next step</p>
          <h3 class="panel-title">Keep the flow moving</h3>
        </div>
        <div class="list-item">
          <strong>Analytics dashboard</strong>
          <p>Open the dashboard to inspect redirect clicks, QR scans, and country breakdowns.</p>
        </div>
        <div class="list-item">
          <strong>Ownership</strong>
          <p>{{ createdLink.ownerUsername ?? 'Anonymous demo' }}</p>
        </div>
        <div class="actions">
          <RouterLink class="button button-primary" :to="dashboardUrl">
            Open analytics
          </RouterLink>
          <RouterLink class="button button-secondary" :to="{ name: 'about' }">
            About the stack
          </RouterLink>
        </div>
      </div>
    </article>
  </section>
</template>
