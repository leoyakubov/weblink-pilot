<script setup lang="ts">
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import LinkFilters from '@/shared/components/LinkFilters.vue';
import PageIntro from '@/shared/components/PageIntro.vue';
import PaginationControls from '@/shared/components/PaginationControls.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import RefreshButton from '@/shared/components/RefreshButton.vue';
import { useAnalyticsView } from './AnalyticsView';
import './AnalyticsView.css';

const {
  filters,
  pagination,
  rows,
  creatorOptions,
  loading,
  errorMessage,
  canFilterByCreator,
  visibleRows,
  loadAnalyticsOverview,
  applyFilters,
  previousPage,
  nextPage,
  scopeLabel,
  ownerLabel,
  ownerRoleLabel,
  formatDate,
} = useAnalyticsView();
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Analytics"
      title="Link analytics"
      description="Compare engagement across the links you can access, then open detailed insights for any link."
    />

    <PanelCard
      eyebrow="Overview"
      title="Performance by link"
      :description="`Showing ${scopeLabel()} with totals, QR scans, visitors, and the latest activity.`"
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="loadAnalyticsOverview" />
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

      <div v-if="loading && !rows.length" class="empty-state">
        <p class="eyebrow">Loading</p>
        <h4 class="card-title">Loading analytics summaries...</h4>
      </div>

      <div v-else-if="visibleRows.length" class="analytics-table" role="table">
        <div class="analytics-table__header" role="row">
          <span>Link</span>
          <span>Owner</span>
          <span>Total</span>
          <span>Redirects</span>
          <span>QR scans</span>
          <span>Visitors</span>
          <span>Last interaction</span>
        </div>

        <div
          v-for="row in visibleRows"
          :key="row.link.code"
          class="analytics-table__row"
          role="row"
        >
          <div class="analytics-table__link">
            <strong>{{ row.link.code }}</strong>
            <span>{{ row.link.shortUrl }}</span>
          </div>
          <span class="analytics-table__owner">
            {{ ownerLabel(row.link) }}
            <small>{{ ownerRoleLabel(row.link).toLowerCase() }}</small>
          </span>
          <strong>{{ row.summary?.totalClicks ?? 'N/A' }}</strong>
          <span>{{ row.summary?.redirectClicks ?? 'N/A' }}</span>
          <span>{{ row.summary?.qrScans ?? 'N/A' }}</span>
          <span>{{ row.summary?.uniqueVisitors ?? 'N/A' }}</span>
          <span>{{ formatDate(row.summary?.lastClickedAt ?? null) }}</span>

          <p v-if="row.error" class="status warning analytics-table__error">
            <span class="status-dot"></span>
            {{ row.error }}
          </p>

          <div class="actions recent-link-actions analytics-table__actions">
            <RouterLink :to="{ name: 'analytics-detail', params: { code: row.link.code } }">
              <Button label="Analytics" icon="pi pi-chart-line" />
            </RouterLink>
            <RouterLink :to="{ name: 'link', params: { code: row.link.code } }">
              <Button
                label="Details"
                icon="pi pi-external-link"
                severity="secondary"
                variant="outlined"
              />
            </RouterLink>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p class="eyebrow">No analytics</p>
        <h4 class="card-title">No links match the current filters.</h4>
      </div>

      <PaginationControls
        v-if="visibleRows.length || pagination.totalElements"
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
  </section>
</template>
