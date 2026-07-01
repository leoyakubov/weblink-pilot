<script setup lang="ts">
import './HistoryView.css';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import LinkFilters from '@/shared/components/LinkFilters.vue';
import LinkList from '@/shared/components/LinkList.vue';
import PageIntro from '@/shared/components/PageIntro.vue';
import PaginationControls from '@/shared/components/PaginationControls.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import QrCodeModal from '@/shared/components/QrCodeModal.vue';
import RefreshButton from '@/shared/components/RefreshButton.vue';
import { useHistoryView } from './HistoryView';

const {
  filters,
  pagination,
  links,
  creatorOptions,
  loading,
  errorMessage,
  qrModalUrl,
  qrModalTitle,
  hasLinks,
  canFilterByCreator,
  scopeLabel,
  refresh,
  applyFilters,
  previousPage,
  nextPage,
  openQrModal,
  closeQrModal,
} = useHistoryView();
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Links"
      title="Saved links"
      description="Manage the links available to you, copy short URLs, open QR codes, and review activity."
    />

    <PanelCard
      eyebrow="Library"
      title="Latest links"
      :description="`Showing ${scopeLabel()} with quick actions for sharing and analytics.`"
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="refresh" />
      </template>

      <LinkFilters
        v-model:owner-scope="filters.ownerScope"
        v-model:expiration-scope="filters.expirationScope"
        v-model:creator="filters.creator"
        :creator-options="creatorOptions"
        :show-admin-filters="canFilterByCreator"
        :loading="loading"
        @apply="applyFilters"
      />

      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <LinkList
        v-if="hasLinks"
        :links="links"
        copy-key-prefix="links"
        @open-qr="(item) => openQrModal(item.qrCodeUrl, item.code)"
      />

      <div v-else class="empty-state">
        <p class="eyebrow">No links yet</p>
        <h4 class="card-title">Create a link first and it will appear here.</h4>
        <p class="muted">Saved links appear here as soon as they are created.</p>
        <RouterLink to="/">
          <Button label="Create link" icon="pi pi-plus" />
        </RouterLink>
      </div>

      <PaginationControls
        v-if="hasLinks || pagination.totalElements"
        :page="pagination.page"
        :size="pagination.size"
        :total-elements="pagination.totalElements"
        :total-pages="pagination.totalPages"
        :first="pagination.first"
        :last="pagination.last"
        :loading="loading"
        @previous="previousPage"
        @next="nextPage"
      />
    </PanelCard>

    <QrCodeModal
      :visible="Boolean(qrModalUrl)"
      :title="qrModalTitle"
      :url="qrModalUrl"
      @close="closeQrModal"
    />
  </section>
</template>
