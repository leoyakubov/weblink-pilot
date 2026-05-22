<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { buildApiBaseUrl, getLink, getRedirectPreview } from '@/lib/api'
import { copyText } from '@/lib/clipboard'
import { loadSettings } from '@/lib/settings'
import type { LinkResponse, RedirectPreviewResponse } from '@/types'

const route = useRoute()
const settings = loadSettings()

const link = ref<LinkResponse | null>(null)
const preview = ref<RedirectPreviewResponse | null>(null)
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
    const [details, redirectPreview] = await Promise.all([
      getLink(codeValue, settings),
      getRedirectPreview(codeValue, settings),
    ])
    link.value = details
    preview.value = redirectPreview
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
              <span class="label">Clicks</span>
            </div>
            <div class="metric">
              <span class="value">{{ link.expiresAt ? 'Set' : 'Open' }}</span>
              <span class="label">Expiration</span>
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
          <p class="eyebrow">QR and debug</p>
          <h3 class="panel-title">Scan-friendly output</h3>
        </div>

        <figure v-if="link">
          <img class="qr-image" :src="qrImage" :alt="`QR code for ${link.code}`" />
        </figure>

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
