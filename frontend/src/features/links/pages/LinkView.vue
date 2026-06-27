<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import Button from 'primevue/button';
import { buildApiBaseUrl } from '@/shared/services/http';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import { isAdminUser } from '@/features/auth/services/auth.service';
import { loadSettings } from '@/shared/services/settings';
import { getLink } from '@/features/links/repositories/link.repository';
import type { LinkResponse } from '@/shared/types/api';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import QrCodeModal from '@/shared/components/common/QrCodeModal.vue';

const route = useRoute();
const settings = loadSettings();

const link = ref<LinkResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
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

  try {
    link.value = await getLink(codeValue, settings);
  } catch (error) {
    link.value = null;
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

async function shareUrl(url: string, label: string, title: string) {
  try {
    const shareApi = window.navigator as {
      share?: (data: { title: string; text: string; url: string }) => Promise<void>;
    };

    if (shareApi.share) {
      await shareApi.share({
        title,
        text: title,
        url,
      });
      return;
    }

    await copy(url, label);
  } catch {
    // Native share can be cancelled by the user; keep the page state unchanged.
  }
}

const qrImage = computed(() => link.value?.qrCodeUrl ?? '');

function closeQrModal() {
  qrModalUrl.value = '';
  qrModalTitle.value = '';
}

function formatDate(value: string | null, fallback = 'Never') {
  if (!value) {
    return fallback;
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
      eyebrow="Link details"
      :title="`Details of &quot;${code}&quot;`"
      description="Review the destination, ownership, lifetime, QR code, and sharing actions for this link."
    />

    <div class="page-grid link-detail-top-grid">
      <PanelCard
        eyebrow="Link"
        title="Main details"
        description="Core information for this short URL."
      >
        <p v-if="loading" class="help-text">Loading link details...</p>
        <p v-else-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>

        <template v-else-if="link">
          <dl class="detail-list detail-list--link-detail">
            <div class="form-field--with-popover">
              <dt>Short URL</dt>
              <dd>{{ link.shortUrl }}</dd>
            </div>
            <div>
              <dt>Target URL</dt>
              <dd>{{ link.originalUrl }}</dd>
            </div>
            <div>
              <dt>Owner</dt>
              <dd>{{ link.ownerUsername ?? 'anonymous' }}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{{ formatDate(link.createdAt) }}</dd>
            </div>
            <div>
              <dt>Expires</dt>
              <dd>{{ formatDate(link.expiresAt) }}</dd>
            </div>
          </dl>

          <div class="actions recent-link-actions link-detail-actions">
            <RouterLink :to="{ name: 'analytics-detail', params: { code: link.code } }">
              <Button
                type="button"
                label="Analytics"
                icon="pi pi-chart-line"
                severity="secondary"
                variant="outlined"
              />
            </RouterLink>
            <Button
              type="button"
              :label="isCopied('link-short') ? 'Copied' : 'Copy'"
              :icon="isCopied('link-short') ? 'pi pi-check' : 'pi pi-copy'"
              severity="secondary"
              variant="outlined"
              @click="copy(link.shortUrl, 'link-short')"
            />
            <Button
              type="button"
              label="Open"
              icon="pi pi-arrow-right"
              severity="secondary"
              variant="outlined"
              @click="openExternal(link.shortUrl)"
            />
            <Button
              type="button"
              :label="isCopied('share-link') ? 'Share URL copied' : 'Share'"
              :icon="isCopied('share-link') ? 'pi pi-check' : 'pi pi-share-alt'"
              severity="secondary"
              variant="outlined"
              @click="shareUrl(link.shortUrl, 'share-link', `WebLinkPilot link: ${link.code}`)"
            />
            <Button
              v-if="canSeePreview"
              type="button"
              label="Preview JSON"
              icon="pi pi-code"
              severity="secondary"
              variant="outlined"
              @click="openExternal(buildApiBaseUrl(`/urls/${link.code}/preview`, settings))"
            />
          </div>
        </template>
      </PanelCard>

      <PanelCard
        eyebrow="QR"
        title="QR code"
        description="Use this code for slides, printouts, or quick phone sharing."
      >
        <div v-if="link" class="qr-panel-body">
          <figure class="compact-figure link-detail-qr-figure">
            <img
              class="qr-image link-detail-qr-image"
              :src="qrImage"
              :alt="`QR code for ${link.code}`"
            />
          </figure>
        </div>

        <div class="actions recent-link-actions link-detail-actions" v-if="link">
          <Button
            type="button"
            :label="isCopied('link-qr') ? 'Copied' : 'Copy QR'"
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
          <Button
            type="button"
            :label="isCopied('share-qr') ? 'Copied' : 'Share QR'"
            :icon="isCopied('share-qr') ? 'pi pi-check' : 'pi pi-share-alt'"
            severity="secondary"
            variant="outlined"
            @click="shareUrl(link.qrCodeUrl, 'share-qr', `WebLinkPilot QR: ${link.code}`)"
          />
        </div>
      </PanelCard>
    </div>

    <QrCodeModal
      :visible="Boolean(qrModalUrl)"
      :title="qrModalTitle"
      :url="qrModalUrl"
      @close="closeQrModal"
    />
  </section>
</template>
