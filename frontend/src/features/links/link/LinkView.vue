<script setup lang="ts">
import './LinkView.css';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import PageIntro from '@/shared/components/PageIntro.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import QrCodeModal from '@/shared/components/QrCodeModal.vue';
import { useLinkView } from './LinkView';

const {
  link,
  aiMetadata,
  loading,
  regeneratingAi,
  errorMessage,
  qrModalUrl,
  qrModalTitle,
  buildApiBaseUrl,
  copy,
  isCopied,
  settings,
  code,
  canSeePreview,
  aiMetadataStatus,
  aiMetadataTags,
  regenerateAiLabel,
  openExternal,
  openQrModal,
  regenerateAiMetadata,
  shareUrl,
  qrImage,
  closeQrModal,
  formatDate,
} = useLinkView();
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

          <div class="ai-metadata-card">
            <div class="ai-metadata-card__heading">
              <div>
                <p class="recent-link-label">AI enrichment</p>
                <strong>{{ aiMetadata?.title ?? 'Metadata is being prepared' }}</strong>
              </div>
              <span class="ai-metadata-status">{{ aiMetadataStatus.toLowerCase() }}</span>
            </div>
            <p class="ai-metadata-summary">
              {{
                aiMetadata?.summary ??
                'WeblinkPilot enriches new links asynchronously with a readable title, category, tags, and a suggested alias.'
              }}
            </p>
            <div class="ai-metadata-tags">
              <span v-if="aiMetadata?.category">{{ aiMetadata.category }}</span>
              <span v-for="tag in aiMetadataTags" :key="tag">#{{ tag }}</span>
            </div>
          </div>

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
              :label="isCopied('share-link') ? 'Copied' : 'Share'"
              :icon="isCopied('share-link') ? 'pi pi-check' : 'pi pi-share-alt'"
              severity="secondary"
              variant="outlined"
              @click="shareUrl(link.shortUrl, 'share-link', `WeblinkPilot link: ${link.code}`)"
            />
            <Button
              v-if="canSeePreview"
              type="button"
              label="JSON"
              icon="pi pi-code"
              severity="secondary"
              variant="outlined"
              @click="openExternal(buildApiBaseUrl(`/urls/${link.code}/preview`, settings))"
            />
            <Button
              type="button"
              :label="regenerateAiLabel"
              :icon="regeneratingAi ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'"
              severity="secondary"
              variant="outlined"
              :disabled="regeneratingAi"
              @click="regenerateAiMetadata"
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
            label="QR code"
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
            @click="shareUrl(link.qrCodeUrl, 'share-qr', `WeblinkPilot QR: ${link.code}`)"
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
