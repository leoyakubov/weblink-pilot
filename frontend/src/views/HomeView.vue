<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import CopyActionButton from '@/components/CopyActionButton.vue'
import { buildApiBaseUrl, createLink, getCurrentUser, login, register } from '@/lib/api'
import { defaultSettings, loadSettings, saveSettings } from '@/lib/settings'
import type { ApiSettings, AuthCredentialsRequest, CreateLinkRequest, LinkResponse, UserProfileResponse } from '@/types'

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

const connectionStatus = computed(() => {
  if (!settings.apiBaseUrl) {
    return 'API base URL missing'
  }
  if (currentUser.value) {
    return `Signed in as ${currentUser.value.username} (${currentUser.value.role})`
  }
  return `${settings.apiBaseUrl} - guest mode ready for demo links`
})

const quickStats = computed(() => [
  { value: 'QR', label: 'PNG preview endpoint' },
  { value: '302', label: 'Redirect response' },
  { value: 'JWT', label: 'Guest or signed-in create flow' },
  { value: 'Vue 3', label: 'Mobile-first frontend' },
])

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
)

const dashboardUrl = computed(() =>
  createdLink.value ? { name: 'dashboard', query: { code: createdLink.value.code } } : '/',
)

function syncSettings() {
  saveSettings({
    apiBaseUrl: settings.apiBaseUrl || defaultSettings().apiBaseUrl,
    authToken: settings.authToken,
  })
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
  refreshSession()
})
</script>

<template>
  <section class="page-grid two-col">
    <div class="stack">
      <article class="card">
        <div class="card-inner hero-copy">
          <p class="eyebrow">Create flow</p>
          <h2 class="hero-title">Short links that feel like a product, not a toy.</h2>
          <p class="hero-note">
            This shell is wired for link creation, preview, QR output, guest demo links, and signed-in user ownership.
            It is designed to be comfortable on a phone and still read well on a bigger screen.
          </p>

          <div class="hero-badges">
            <span class="badge"><strong>Mobile-first</strong> layout</span>
            <span class="badge"><strong>Vue 3</strong> + Vite</span>
            <span class="badge"><strong>JWT</strong> guest or owned links</span>
          </div>

          <div class="metrics-grid">
            <div v-for="stat in quickStats" :key="stat.label" class="metric">
              <span class="value">{{ stat.value }}</span>
              <span class="label">{{ stat.label }}</span>
            </div>
          </div>
        </div>
      </article>

      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">Account</p>
            <h3 class="panel-title">Guest or signed-in mode</h3>
          </div>

          <label class="form-field">
            <span class="field-label">API base URL</span>
            <input
              v-model="settings.apiBaseUrl"
              class="input"
              type="url"
              placeholder="http://localhost:8080/api/v1"
              @blur="syncSettings"
            />
          </label>

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
            <button class="button button-primary" type="button" :disabled="authBusy" @click="authenticate('login')">
              {{ authBusy ? 'Signing in...' : 'Sign in' }}
            </button>
            <button class="button button-secondary" type="button" :disabled="authBusy" @click="authenticate('register')">
              Register
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
            Guests can create anonymous demo links. Signing in makes new links owned by your account and unlocks your private history and analytics.
          </p>
        </div>
      </article>
    </div>

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Create short link</p>
          <h3 class="panel-title">URL shortener form</h3>
          <p class="help-text">
            Anonymous demo links still work. If you are signed in, the link will belong to your account.
            Preview and QR URLs stay public.
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
    </article>
  </section>

  <section v-if="createdLink" class="page-grid two-col" style="margin-top: 1rem;">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Created link</p>
          <h3 class="panel-title">Share card</h3>
        </div>

        <div class="link-preview">
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
      </div>
    </article>

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
  </section>
</template>
