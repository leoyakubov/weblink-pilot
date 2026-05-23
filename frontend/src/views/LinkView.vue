<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { buildApiBaseUrl, getAnalyticsSummary, getLink, getRedirectPreview } from '@/lib/api'
import { countryLabel } from '@/lib/countries'
import { copyText } from '@/lib/clipboard'
import { loadSettings } from '@/lib/settings'
import type { AnalyticsSummaryResponse, LinkResponse, RedirectPreviewResponse } from '@/types'

const route = useRoute()
const settings = loadSettings()

const link = ref<LinkResponse | null>(null)
const preview = ref<RedirectPreviewResponse | null>(null)
const analytics = ref<AnalyticsSummaryResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const code = computed(() => String(route.params.code ?? ''))

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

function copy(value: string) {
  return copyText(value)
}

const qrImage = computed(() => link.value?.qrCodeUrl ?? '')

function formatDate(value: string | null) {
  if (!value) {
    return 'No clicks yet'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <section class="page-grid two-col">
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
          </div>
          <div class="list-item">
            <strong>Target URL</strong>
            <p>{{ link.originalUrl }}</p>
          </div>
          <div class="list-item">
            <strong>Preview</strong>
            <p>{{ preview?.locationHeader ?? 'Preview not loaded yet' }}</p>
          </div>

          <div class="actions">
            <button class="button button-primary" type="button" @click="copy(link.shortUrl)">
              Copy short URL
            </button>
            <button class="button button-secondary" type="button" @click="openExternal(link.shortUrl)">
              Open redirect
            </button>
            <button
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

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">QR and analytics</p>
          <h3 class="panel-title">Scan-friendly output</h3>
        </div>

        <figure v-if="link">
          <img class="qr-image" :src="qrImage" :alt="`QR code for ${link.code}`" />
        </figure>

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
            <p>{{ formatDate(analytics.lastClickAt) }}</p>
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
                <strong>{{ countryLabel(country.country) }}</strong>
                <p>{{ country.clicks }} clicks</p>
              </div>
            </div>
          </div>
        </div>

        <div v-if="preview" class="stack">
          <div class="list-item">
            <strong>Preview endpoint</strong>
            <p>{{ buildApiBaseUrl(`/urls/${preview.code}/preview`, settings) }}</p>
          </div>
          <pre class="code-block">{{ JSON.stringify(preview, null, 2) }}</pre>
        </div>

        <div class="actions" v-if="link">
          <button class="button button-primary" type="button" @click="copy(link.qrCodeUrl)">
            Copy QR URL
          </button>
          <button class="button button-secondary" type="button" @click="openExternal(link.qrCodeUrl)">
            Open QR image
          </button>
        </div>
      </div>
    </article>
  </section>
</template>
