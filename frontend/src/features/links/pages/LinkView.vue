<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import Button from 'primevue/button';
import { ApiRequestError, buildApiBaseUrl } from '@/shared/services/http';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import { isAdminUser } from '@/features/auth/services/auth.service';
import { loadSettings } from '@/shared/services/settings';
import {
  getAnalyticsSummary,
  getLink,
  getRedirectPreview,
} from '@/features/links/repositories/link.repository';
import type {
  AnalyticsSummaryResponse,
  LinkResponse,
  RedirectPreviewResponse,
} from '@/shared/types/api';
import AnalyticsSummaryPanel from '@/shared/components/common/AnalyticsSummaryPanel.vue';
import HelpTooltip from '@/shared/components/common/HelpTooltip.vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import QrCodeModal from '@/shared/components/common/QrCodeModal.vue';

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
        analyticsMessage.value = 'Analytics are available only to the link owner or an admin user.';
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
  <section class="page-grid compact-detail">
    <PageIntro
      eyebrow="Link detail"
      :title="`Code: ${code}`"
      description="Review the short link target, ownership, QR code, redirect preview, and analytics summary."
    />

    <div class="page-grid two-col">
      <PanelCard
        eyebrow="Link detail"
        title="Overview"
        description="Core metadata and quick actions for this short link."
      >
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

          <dl class="detail-list detail-list--link-detail">
            <div class="form-field--with-popover">
              <dt>
                Short URL
                <HelpTooltip button-label="Toggle short URL help">
                  Short URLs are generated randomly unless you choose a custom alias.
                </HelpTooltip>
              </dt>
              <dd>{{ link.shortUrl }}</dd>
            </div>
            <div>
              <dt>Target URL</dt>
              <dd>{{ link.originalUrl }}</dd>
            </div>
            <div>
              <dt>Owner</dt>
              <dd>{{ link.ownerUsername ?? 'Anonymous demo' }}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{{ formatDate(link.createdAt) }}</dd>
            </div>
            <div>
              <dt>Expires</dt>
              <dd>{{ formatDate(link.expiresAt) }}</dd>
            </div>
            <div>
              <dt>Preview</dt>
              <dd>{{ preview?.locationHeader ?? 'Preview not loaded yet' }}</dd>
            </div>
          </dl>

          <div class="actions link-detail-actions">
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
      </PanelCard>

      <div class="stack">
        <PanelCard
          eyebrow="QR"
          title="Scan-ready output"
          description="Use this QR code in slides, printouts, or mobile sharing."
        >
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
        </PanelCard>

        <PanelCard
          eyebrow="Analytics"
          title="Analytics from backend"
          description="Loaded from the analytics summary API for this short code. Shows redirect clicks, QR scans, unique visitors, and country data when available."
        >
          <div v-if="analytics" class="stack">
            <AnalyticsSummaryPanel :summary="analytics" />
          </div>

          <p v-else-if="analyticsMessage" class="status warning">
            <span class="status-dot"></span>
            {{ analyticsMessage }}
          </p>
        </PanelCard>
      </div>
    </div>

    <QrCodeModal
      :visible="Boolean(qrModalUrl)"
      :title="qrModalTitle"
      :url="qrModalUrl"
      @close="closeQrModal"
    />
  </section>
</template>
