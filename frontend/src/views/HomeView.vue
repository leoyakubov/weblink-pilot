<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import CopyActionButton from '@/components/CopyActionButton.vue'
import { authState } from '@/lib/auth'
import { buildApiBaseUrl, createLink, listLinks } from '@/lib/api'
import { loadSettings, saveSettings } from '@/lib/settings'
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/types'

const settings = reactive<ApiSettings>(loadSettings())
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://example.com/docs/getting-started',
  customAlias: '',
  expiresAt: '',
})

const createdLink = ref<LinkResponse | null>(null)
const recentLinks = ref<LinkResponse[]>([])
const loadingRecent = ref(false)
const recentError = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const submitting = ref(false)

const userStatus = computed(() => authState.currentUser
  ? `Signed in as ${authState.currentUser.username} (${authState.currentUser.role})`
  : 'Guest mode ready for demo links')

const recentTitle = computed(() => authState.currentUser ? 'Your Recent Links' : 'Recent Links')

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
)

const dashboardUrl = computed(() =>
  createdLink.value ? { name: 'dashboard', query: { code: createdLink.value.code } } : '/',
)

function syncSettings() {
  saveSettings(settings)
}

async function refreshRecent() {
  loadingRecent.value = true
  recentError.value = ''

  try {
    recentLinks.value = await listLinks(5, settings)
  } catch (error) {
    recentLinks.value = []
    recentError.value = error instanceof Error ? error.message : 'Could not load recent links'
  } finally {
    loadingRecent.value = false
  }
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
    await refreshRecent()
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
  refreshRecent()
})

watch(
  () => authState.currentUser?.username,
  () => {
    refreshRecent()
  },
)
</script>

<template>
  <section class="landing-layout stack">
    <article class="card hero-card">
      <div class="card-inner hero-grid">
        <div class="hero-copy">
          <p class="eyebrow">Link management</p>
          <h2 class="hero-title">Short links, QR codes, and analytics in one clean workspace.</h2>
          <p class="hero-note">
            WebLinkPilot is built for fast sharing. Create a demo link instantly, or sign in to keep ownership and
            private history. Redirects, QR scans, and analytics stay public and fast.
          </p>

          <div class="hero-badges">
            <span class="badge"><strong>Guest</strong> demo links</span>
            <span class="badge"><strong>Owned</strong> user links</span>
            <span class="badge"><strong>QR</strong> and analytics</span>
          </div>

          <div class="hero-points">
            <div class="hero-point">
              <strong>Simple start</strong>
              <p>Create a link now, no account required.</p>
            </div>
            <div class="hero-point">
              <strong>Signed-in mode</strong>
              <p>Keep your links owned and revisit them later.</p>
            </div>
          </div>

          <p class="status warning">
            <span class="status-dot"></span>
            {{ userStatus }}
          </p>
        </div>

        <div class="stack">
          <div>
            <p class="eyebrow">Quick create</p>
            <h3 class="panel-title">Shorten a URL</h3>
            <p class="help-text">
              Leave alias blank for a random code. Add one if you want a branded short URL.
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
            </label>

            <label class="form-field">
              <span class="field-label">Expiration</span>
              <input v-model="form.expiresAt" class="input" type="datetime-local" />
            </label>

            <div class="actions">
              <button class="button button-primary" type="submit" :disabled="submitting">
                {{ submitting ? 'Creating...' : 'Create short link' }}
              </button>
              <RouterLink class="button button-secondary" to="/about">
                About
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
          </form>
        </div>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Recent links</p>
            <h3 class="panel-title">{{ recentTitle }}</h3>
          </div>
          <button class="button button-secondary" type="button" :disabled="loadingRecent" @click="refreshRecent">
            {{ loadingRecent ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>

        <p v-if="recentError" class="status error">
          <span class="status-dot"></span>
          {{ recentError }}
        </p>

        <div v-if="recentLinks.length" class="list">
          <div v-for="item in recentLinks" :key="item.code" class="list-item">
            <div class="section-row">
              <div>
                <strong>{{ item.code }}</strong>
                <p>{{ item.shortUrl }}</p>
              </div>
              <span class="footnote">{{ item.ownerUsername ?? 'Anonymous demo' }}</span>
            </div>
            <p class="footnote">{{ item.clickCount }} clicks</p>
            <div class="actions">
              <RouterLink class="button button-primary" :to="{ name: 'link', params: { code: item.code } }">
                Details
              </RouterLink>
              <RouterLink class="button button-secondary" :to="{ name: 'dashboard', query: { code: item.code } }">
                Analytics
              </RouterLink>
              <CopyActionButton :value="item.shortUrl" label="Copy short URL" copied-label="Short URL copied" />
              <button class="button button-secondary" type="button" @click="openExternal(item.qrCodeUrl)">
                Open QR
              </button>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p class="eyebrow">No history yet</p>
          <h4 class="card-title">Create your first short link and it will appear here.</h4>
          <p class="muted">
            Recent links come from the backend, so this section always reflects the latest saved data.
          </p>
        </div>
      </div>
    </article>

    <section v-if="createdLink" class="page-grid two-col">
      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">Created link</p>
            <h3 class="panel-title">Share card</h3>
          </div>

          <div class="list-item">
            <strong>{{ createdLink.shortUrl }}</strong>
            <p>Short URL ready to copy, open, or share.</p>
          </div>
          <div class="list-item">
            <strong>{{ createdLink.ownerUsername ?? 'Anonymous demo' }}</strong>
            <p>Ownership for this link.</p>
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
  </section>
</template>
