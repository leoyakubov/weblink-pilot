<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CopyActionButton from '@/components/CopyActionButton.vue'
import { buildApiBaseUrl, getAnalyticsSummary, getLink, getRedirectPreview } from '@/lib/api'
import { isAdminUser } from '@/lib/auth'
import { countryCodeLabel, countryFlagUrl } from '@/lib/countries'
import { loadSettings } from '@/lib/settings'
import type { AnalyticsSummaryResponse, LinkResponse, RedirectPreviewResponse } from '@/types'

const route = useRoute()
const settings = loadSettings()

const link = ref<LinkResponse | null>(null)
const preview = ref<RedirectPreviewResponse | null>(null)
const analytics = ref<AnalyticsSummaryResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const qrModalUrl = ref('')
const qrModalTitle = ref('')

const code = computed(() => String(route.params.code ?? ''))
const canSeePreview = computed(() => isAdminUser())

async function load(codeValue: string) {
  if (!codeValue) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [details, redirectPreview, analyticsSummary] = await Promise.all([
      getLink(codeValue, settings),
      getRedirectPreview(codeValue, settings),
      getAnalyticsSummary(codeValue, settings),
    ])

    link.value = details
    preview.value = redirectPreview
    analytics.value = analyticsSummary
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not load link details'
  } finally {
    loading.value = false
  }
}

onMounted(() => load(code.value))
watch(code, value => load(value))

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

function openQrModal(url: string, title: string) {
  qrModalUrl.value = url
  qrModalTitle.value = title
}

function closeQrModal() {
  qrModalUrl.value = ''
  qrModalTitle.value = ''
}

const qrImage = computed(() => link.value?.qrCodeUrl ?? '')

function formatDate(value: string | null) {
  if (!value) {
    return 'Never'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <section class="page-grid two-col compact-detail">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Link detail</p>
          <h3 class="panel-title">Code: {{ code }}</h3>
        </div>

        <p v-if="loading" class="help-text">Loading the link from the backend...</p>
        <p v-else-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>

        <template v-else-if="link">
          <div class="grid-2">
            <div class="metric">
              <span class="value">{{ link.clickCount }}</span>
              <span class="label">Total interactions</span>
            </div>
            <div class="metric">
              <span class="value">{{ analytics?.redirectClicks ?? 0 }}</span>
              <span class="label">Redirect clicks</span>
            </div>
            <div class="metric">
              <span class="value">{{ analytics?.qrScans ?? 0 }}</span>
              <span class="label">QR scans</span>
            </div>
            <div class="metric">
              <span class="value">{{ analytics?.uniqueVisitors ?? 0 }}</span>
              <span class="label">Unique visitors</span>
            </div>
          </div>

          <div class="list-item">
            <strong>Short URL</strong>
            <p>{{ link.shortUrl }}</p>
            <p class="help-text">Short URLs are generated randomly unless you choose a custom alias.</p>
          </div>
          <div class="list-item">
            <strong>Target URL</strong>
            <p>{{ link.originalUrl }}</p>
          </div>
          <div class="list-item">
            <strong>Owner</strong>
            <p>{{ link.ownerUsername ?? 'Anonymous demo' }}</p>
          </div>
          <div class="list-item-meta">
            <span>Created: {{ formatDate(link.createdAt) }}</span>
            <span>Expires: {{ formatDate(link.expiresAt) }}</span>
          </div>
          <div class="list-item">
            <strong>Preview</strong>
            <p>{{ preview?.locationHeader ?? 'Preview not loaded yet' }}</p>
          </div>

          <div class="actions">
            <CopyActionButton
              :value="link.shortUrl"
              label="Copy short URL"
              copied-label="Short URL copied"
              variant="primary"
            />
            <button class="button button-secondary" type="button" @click="openExternal(link.shortUrl)">
              Open redirect
            </button>
            <button
              v-if="canSeePreview"
              class="button button-secondary"
              type="button"
              @click="openExternal(buildApiBaseUrl(`/urls/${link.code}/preview`, settings))"
            >
              Open preview JSON
            </button>
          </div>
        </template>
      </div>
    </article>

    <div class="stack">
      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">QR</p>
            <h3 class="panel-title">Scan-ready output</h3>
          </div>

          <figure v-if="link" class="compact-figure">
            <img class="qr-image qr-image--compact" :src="qrImage" :alt="`QR code for ${link.code}`" />
          </figure>

          <div class="actions" v-if="link">
            <CopyActionButton
              :value="link.qrCodeUrl"
              label="Copy QR URL"
              copied-label="QR URL copied"
              variant="primary"
            />
            <button class="button button-secondary" type="button" @click="openQrModal(link.qrCodeUrl, link.code)">
              Open QR
            </button>
          </div>
        </div>
      </article>

      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">Analytics</p>
            <h3 class="panel-title">Click breakdown</h3>
          </div>

          <div v-if="analytics" class="stack">
            <div class="grid-2">
              <div class="metric">
                <span class="value">{{ analytics.totalClicks }}</span>
                <span class="label">Total interactions</span>
              </div>
              <div class="metric">
                <span class="value">{{ analytics.redirectClicks }}</span>
                <span class="label">Redirect clicks</span>
              </div>
              <div class="metric">
                <span class="value">{{ analytics.qrScans }}</span>
                <span class="label">QR scans</span>
              </div>
              <div class="metric">
                <span class="value">{{ analytics.uniqueVisitors }}</span>
                <span class="label">Unique visitors</span>
              </div>
            </div>

            <div class="list-item">
              <strong>Last click</strong>
              <p>{{ formatDate(analytics.lastClickedAt) }}</p>
            </div>
            <div class="list-item">
              <strong>Last referrer</strong>
              <p>{{ analytics.lastReferrer ?? 'No referrer captured yet' }}</p>
            </div>
            <div class="list-item">
              <strong>Browser / device</strong>
              <p>
                {{ analytics.lastBrowserFamily ?? 'Unknown browser' }} - {{ analytics.lastDeviceType ?? 'Unknown device' }}
              </p>
            </div>

            <div v-if="analytics.topCountries.length" class="stack">
              <div>
                <p class="eyebrow">Top countries</p>
                <h4 class="card-title">Where clicks are coming from</h4>
              </div>
              <div class="list">
                <div v-for="country in analytics.topCountries" :key="country.country" class="list-item">
                  <strong class="country-label">
                    <img
                      v-if="countryFlagUrl(country.country)"
                      class="country-flag"
                      :src="countryFlagUrl(country.country) || ''"
                      :alt="`${countryCodeLabel(country.country)} flag`"
                    />
                    <span>{{ countryCodeLabel(country.country) }}</span>
                  </strong>
                  <p>{{ country.clicks }} clicks</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </article>
    </div>

    <teleport to="body">
      <Transition name="session-notice">
        <div v-if="qrModalUrl" class="modal-backdrop" @click.self="closeQrModal">
          <div class="modal-card card">
            <div class="card-inner stack">
              <div class="section-row">
                <div>
                  <p class="eyebrow">QR code</p>
                  <h3 class="panel-title">{{ qrModalTitle }}</h3>
                </div>
                <button class="button button-secondary button-small" type="button" @click="closeQrModal">Close</button>
              </div>

              <img class="qr-image qr-image--compact modal-qr" :src="qrModalUrl" :alt="`QR code for ${qrModalTitle}`" />
            </div>
          </div>
        </div>
      </Transition>
    </teleport>
  </section>
</template>
