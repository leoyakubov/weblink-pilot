<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import Button from 'primevue/button';
import LinkFilters from '@/features/links/components/LinkFilters.vue';
import { ApiRequestError } from '@/shared/services/http';
import { isAdminUser } from '@/features/auth/services/auth.service';
import { loadSettings } from '@/shared/services/settings';
import {
  getAnalyticsSummary,
  getLinkCreatorOptions,
  listLinks,
} from '@/features/links/repositories/link.repository';
import type {
  AnalyticsSummaryResponse,
  LinkCreatorOptionResponse,
  LinkResponse,
} from '@/shared/types/api';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';

type AnalyticsRow = {
  link: LinkResponse;
  summary: AnalyticsSummaryResponse | null;
  error: string;
};

const route = useRoute();
const router = useRouter();
const settings = loadSettings();

const filters = reactive({
  ownerScope: String(route.query.scope ?? 'all'),
  creator: String(route.query.creator ?? ''),
});

const rows = ref<AnalyticsRow[]>([]);
const creatorOptions = ref<LinkCreatorOptionResponse[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const canFilterByCreator = computed(() => isAdminUser());

const visibleRows = computed(() => rows.value);

function backendCreatorFilter() {
  if (!canFilterByCreator.value) {
    return '';
  }

  const exactCreator = filters.creator.trim();
  if (exactCreator) {
    return exactCreator;
  }

  return '';
}

function backendOwnerRoleFilter() {
  if (!canFilterByCreator.value || filters.creator.trim()) {
    return '';
  }

  const roles: Record<string, string> = {
    admins: 'ADMIN',
    users: 'USER',
    anonymous: 'ANONYMOUS',
  };
  return roles[filters.ownerScope] ?? '';
}

async function loadAnalyticsOverview() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const creator = backendCreatorFilter();
    const ownerRole = backendOwnerRoleFilter();
    const links = await listLinks(20, settings, creator, ownerRole);
    const analyticsRows = await Promise.all(
      links.map(async (link) => {
        try {
          return {
            link,
            summary: await getAnalyticsSummary(link.code, settings),
            error: '',
          };
        } catch (error) {
          const forbidden =
            error instanceof ApiRequestError && (error.status === 401 || error.status === 403);
          return {
            link,
            summary: null,
            error: forbidden
              ? 'Analytics are not available for this link.'
              : error instanceof Error
                ? error.message
                : 'Could not load analytics.',
          };
        }
      }),
    );
    rows.value = analyticsRows;
    router.replace({
      query: {
        ...(canFilterByCreator.value && filters.ownerScope !== 'all'
          ? { scope: filters.ownerScope }
          : {}),
        ...(canFilterByCreator.value && filters.creator.trim()
          ? { creator: filters.creator.trim() }
          : {}),
      },
    });
  } catch (error) {
    rows.value = [];
    errorMessage.value = error instanceof Error ? error.message : 'Could not load analytics.';
  } finally {
    loading.value = false;
  }
}

async function loadCreatorOptions() {
  if (!canFilterByCreator.value) {
    creatorOptions.value = [];
    return;
  }

  creatorOptions.value = await getLinkCreatorOptions(settings);
}

function scopeLabel() {
  if (!canFilterByCreator.value) {
    return 'available links';
  }

  if (filters.creator.trim()) {
    return `creator "${filters.creator.trim()}"`;
  }

  const labels: Record<string, string> = {
    all: 'all links',
    admins: 'admin-owned links',
    users: 'user-owned links',
    anonymous: 'anonymous links',
  };
  return labels[filters.ownerScope] ?? 'all links';
}

function ownerLabel(link: LinkResponse) {
  return link.ownerUsername ?? 'anonymous';
}

function ownerRoleLabel(link: LinkResponse) {
  return link.ownerRole ?? (link.ownerUsername ? 'USER' : 'ANONYMOUS');
}

function formatDate(value: string | null) {
  if (!value) {
    return 'No interactions yet';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(async () => {
  await loadCreatorOptions();
  void loadAnalyticsOverview();
});
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Analytics"
      title="Link analytics"
      description="Compare traffic, QR scans, and visitor signals across links you can access."
    />

    <PanelCard
      eyebrow="Links"
      title="Analytics by link"
      :description="`Showing ${scopeLabel()} with analytics summaries loaded from the backend.`"
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="loadAnalyticsOverview" />
      </template>

      <LinkFilters
        v-if="canFilterByCreator"
        v-model:owner-scope="filters.ownerScope"
        v-model:creator="filters.creator"
        :creator-options="creatorOptions"
        :loading="loading"
        @apply="loadAnalyticsOverview"
      />

      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <div v-if="loading && !rows.length" class="empty-state">
        <p class="eyebrow">Loading</p>
        <h4 class="card-title">Loading link analytics from the backend...</h4>
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
              <Button label="Detailed analytics" icon="pi pi-chart-line" />
            </RouterLink>
            <RouterLink :to="{ name: 'link', params: { code: row.link.code } }">
              <Button
                label="Link details"
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
    </PanelCard>
  </section>
</template>
