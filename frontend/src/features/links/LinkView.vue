<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import Button from 'primevue/button';
import {
  ApiRequestError,
  buildApiBaseUrl,
  getAnalyticsSummary,
  getLink,
  getRedirectPreview,
} from '@/lib/api';
import { useCopyAction } from '@/lib/copy-action';
import { isAdminUser } from '@/lib/auth';
import { loadSettings } from '@/lib/settings';
import type { AnalyticsSummaryResponse, LinkResponse, RedirectPreviewResponse } from '@/types';
import AnalyticsSummaryPanel from '@/shared/components/AnalyticsSummaryPanel.vue';

const route = useRoute();
const settings = loadSettings();

const link = ref<LinkResponse | null>(null);
const preview = ref<RedirectPreviewResponse | null>(null);
const analytics = ref<AnalyticsSummaryResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const analyticsMessage = ref('');
const qrModalUrl = ref('');
const qrModalTitle = ref('');
const { copy, isCopied } = useCopyAction();

const code = computed(() => String(route.params.code ?? ''));
const canSeePreview = computed(() => isAdminUser());

async function load(codeValue: string) {
  if (!codeValue) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  analyticsMessage.value = '';

  try {
    link.value = await getLink(codeValue, settings);

    const [redirectPreviewResult, analyticsResult] = await Promise.allSettled([
      getRedirectPreview(codeValue, settings),
      getAnalyticsSummary(codeValue, settings),
    ]);

    if (redirectPreviewResult.status === 'fulfilled') {
      preview.value = redirectPreviewResult.value;
    } else {
      preview.value = null;
    }

    if (analyticsResult.status === 'fulfilled') {
      analytics.value = analyticsResult.value;
    } else {
      analytics.value = null;
      if (
        analyticsResult.reason instanceof ApiRequestError &&
        analyticsResult.reason.status === 403
      ) {
        analyticsMessage.value =
          'Analytics are available only to the link owner or an admin user.';
      } else {
        analyticsMessage.value =
          analyticsResult.reason instanceof Error
            ? analyticsResult.reason.message
            : 'Could not load analytics';
      }
    }
  } catch (error) {
    link.value = null;
    preview.value = null;
    analytics.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'Could not load link details';
  } finally {
    loading.value = false;
  }
}

onMounted(() => load(code.value));
watch(code, (value) => load(value));

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
}

function openQrModal(url: string, title: string) {
  qrModalUrl.value = url;
  qrModalTitle.value = title;
}

function closeQrModal() {
  qrModalUrl.value = '';
  qrModalTitle.value = '';
}

const qrImage = computed(() => link.value?.qrCodeUrl ?? '');

function formatDate(value: string | null) {
  if (!value) {
    return 'Never';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
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
            <p class="help-text">
              Short URLs are generated randomly unless you choose a custom alias.
            </p>
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
            <Button
              type="button"
              :label="isCopied('link-short') ? 'Short URL copied' : 'Copy short URL'"
              :icon="isCopied('link-short') ? 'pi pi-check' : 'pi pi-copy'"
              severity="secondary"
              variant="outlined"
              @click="copy(link.shortUrl, 'link-short')"
            />
            <Button
              type="button"
              label="Open redirect"
              icon="pi pi-arrow-right"
              severity="secondary"
              variant="outlined"
              @click="openExternal(link.shortUrl)"
            />
            <Button
              v-if="canSeePreview"
              type="button"
              label="Open preview JSON"
              icon="pi pi-code"
              severity="secondary"
              variant="outlined"
              @click="openExternal(buildApiBaseUrl(`/urls/${link.code}/preview`, settings))"
            />
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
            <img
              class="qr-image qr-image--compact"
              :src="qrImage"
              :alt="`QR code for ${link.code}`"
            />
          </figure>

          <div class="actions" v-if="link">
            <Button
              type="button"
              :label="isCopied('link-qr') ? 'QR URL copied' : 'Copy QR URL'"
              :icon="isCopied('link-qr') ? 'pi pi-check' : 'pi pi-copy'"
              severity="secondary"
              variant="outlined"
              @click="copy(link.qrCodeUrl, 'link-qr')"
            />
            <Button
              type="button"
              label="Open QR"
              icon="pi pi-qrcode"
              severity="secondary"
              variant="outlined"
              @click="openQrModal(link.qrCodeUrl, link.code)"
            />
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
            <AnalyticsSummaryPanel :summary="analytics" />
          </div>

          <p v-else-if="analyticsMessage" class="status warning">
            <span class="status-dot"></span>
            {{ analyticsMessage }}
          </p>
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
                <Button
                  type="button"
                  label="Close"
                  icon="pi pi-times"
                  severity="secondary"
                  variant="text"
                  size="small"
                  @click="closeQrModal"
                />
              </div>

              <img
                class="qr-image qr-image--compact modal-qr"
                :src="qrModalUrl"
                :alt="`QR code for ${qrModalTitle}`"
              />
            </div>
          </div>
        </div>
      </Transition>
    </teleport>
  </section>
</template>
