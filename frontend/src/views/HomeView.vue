<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import CopyActionButton from '@/components/CopyActionButton.vue'
import { buildApiBaseUrl, createLink } from '@/lib/api'
import { defaultSettings, loadSettings, saveSettings } from '@/lib/settings'
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/types'

const settings = reactive<ApiSettings>(loadSettings())
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://example.com/docs/getting-started',
  customAlias: '',
  expiresAt: '',
})

const createdLink = ref<LinkResponse | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const submitting = ref(false)

const connectionStatus = computed(() => {
  if (!settings.apiBaseUrl) {
    return 'API base URL missing'
  }
  return `${settings.apiBaseUrl} - ready for create requests`
})

const quickStats = computed(() => [
  { value: 'QR', label: 'PNG preview endpoint' },
  { value: '302', label: 'Redirect response' },
  { value: 'Auth', label: 'Basic auth create flow' },
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
    username: settings.username,
    password: settings.password,
  })
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
</script>

<template>
  <section class="page-grid two-col">
    <div class="stack">
      <article class="card">
        <div class="card-inner hero-copy">
          <p class="eyebrow">Create flow</p>
          <h2 class="hero-title">Short links that feel like a product, not a toy.</h2>
          <p class="hero-note">
            This shell is wired for link creation, preview, and QR output. It is designed to be comfortable on a phone and still read well on a bigger screen.
          </p>

          <div class="hero-badges">
            <span class="badge"><strong>Mobile-first</strong> layout</span>
            <span class="badge"><strong>Vue 3</strong> + Vite</span>
            <span class="badge"><strong>Backend</strong> on Spring Boot</span>
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
            <p class="eyebrow">Current connection</p>
            <h3 class="panel-title">Backend settings</h3>
          </div>

          <div class="grid-2">
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
            <label class="form-field">
              <span class="field-label">Username</span>
              <input v-model="settings.username" class="input" type="text" placeholder="admin" @blur="syncSettings" />
            </label>
          </div>

          <label class="form-field">
            <span class="field-label">Password</span>
            <input v-model="settings.password" class="input" type="password" placeholder="admin123" @blur="syncSettings" />
          </label>

          <p class="help-text">
            These values are stored locally in the browser so the create form can talk to the Spring Boot backend without retyping credentials every time.
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
            The backend currently expects HTTP Basic auth for create requests. The preview and QR URLs are public.
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
