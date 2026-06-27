<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import { getAdminOverview } from '@/features/monitoring/repositories/monitoring.repository';
import { buildMonitoringLinks } from '@/features/monitoring/services/monitoring.service';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import type { AdminOverviewResponse, ApiSettings } from '@/shared/types/api';

const settings = reactive<ApiSettings>(loadSettings());
const canEditBackendUrl = !import.meta.env.PROD;
const router = useRouter();
const monitoringLinks = buildMonitoringLinks(settings);
const overview = ref<AdminOverviewResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const saved = ref(false);
const backendLinks = computed(() => [
  {
    label: 'Health',
    description: 'Check whether the backend is healthy and ready.',
    href: monitoringLinks.backendHealthUrl,
  },
  {
    label: 'Info',
    description: 'View build and application metadata.',
    href: monitoringLinks.backendInfoUrl,
  },
  {
    label: 'Metrics',
    description: 'Inspect raw application metrics.',
    href: monitoringLinks.backendMetricsUrl,
  },
  {
    label: 'Prometheus',
    description: 'Open the metrics feed used by Prometheus.',
    href: monitoringLinks.backendPrometheusUrl,
  },
]);
const localStackLinks = computed(() =>
  monitoringLinks.showLocalStack
    ? [
        {
          label: 'Prometheus',
          description: 'Query local metrics from the Docker stack.',
          href: monitoringLinks.prometheusUrl,
        },
        {
          label: 'Grafana',
          description: 'Open local dashboards for the demo stack.',
          href: monitoringLinks.grafanaUrl,
        },
      ]
    : [],
);

async function refresh() {
  loading.value = true;
  errorMessage.value = '';

  try {
    overview.value = await getAdminOverview(settings);
  } catch (error) {
    overview.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'Could not load admin overview';
  } finally {
    loading.value = false;
  }
}

function saveBaseUrl() {
  saveSettings(settings);
  saved.value = true;
  window.setTimeout(() => {
    saved.value = false;
  }, 1500);
}

function resetBrowserSettings() {
  router.push({ name: 'settings-reset' });
}

onMounted(() => {
  refresh();
});
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Monitoring"
      title="Admin monitoring"
      description="Review demo totals, open operational tools, and adjust local API settings."
    />

    <div class="page-grid two-col monitoring-card-row">
      <PanelCard
        eyebrow="Overview"
        title="System overview"
        description="A quick snapshot of users, links, ownership, and click volume."
      >
        <template #actions>
          <RefreshButton :loading="loading" @refresh="refresh" />
        </template>

        <p class="help-text">
          Admin-only view for checking the current demo data and overall usage.
        </p>

        <p v-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>

        <dl
          v-if="overview"
          class="detail-list detail-list--link-detail detail-list--analytics monitoring-overview-list"
        >
          <div>
            <dt>Total users</dt>
            <dd>{{ overview.totalUsers }}</dd>
          </div>
          <div>
            <dt>Admin users</dt>
            <dd>{{ overview.adminUsers }}</dd>
          </div>
          <div>
            <dt>Total links</dt>
            <dd>{{ overview.totalLinks }}</dd>
          </div>
          <div>
            <dt>Total clicks</dt>
            <dd>{{ overview.totalClicks }}</dd>
          </div>
          <div>
            <dt>Owned links</dt>
            <dd>{{ overview.ownedLinks }}</dd>
          </div>
          <div>
            <dt>anonymous links</dt>
            <dd>{{ overview.anonymousLinks }}</dd>
          </div>
        </dl>
      </PanelCard>

      <PanelCard
        eyebrow="Settings"
        title="Settings"
        description="Change the API URL used by this browser during local development."
      >
        <div class="section-row about-toolbar">
          <div class="about-toolbar__copy">
            <p class="eyebrow about-toolbar__eyebrow">Browser tools</p>
            <p class="help-text">
              Clear saved browser state if the app is pointing at stale local settings.
            </p>
          </div>

          <div class="actions">
            <Button
              type="button"
              label="Reset saved settings"
              severity="warning"
              icon="pi pi-refresh"
              data-testid="reset-browser-settings"
              @click="resetBrowserSettings"
            />
          </div>
        </div>

        <template v-if="canEditBackendUrl">
          <label class="form-field">
            <span class="field-label">API base URL</span>
            <InputText
              v-model="settings.apiBaseUrl"
              type="url"
              placeholder="http://localhost:8080/api/v1"
              fluid
            />
          </label>

          <div class="actions">
            <Button
              type="button"
              label="Save settings"
              icon="pi pi-save"
              data-testid="save-settings"
              @click="saveBaseUrl"
            />
          </div>
        </template>

        <dl v-else class="detail-list">
          <div>
            <dt>API base URL</dt>
            <dd>{{ settings.apiBaseUrl }}</dd>
          </div>
        </dl>

        <p v-if="saved" class="status">
          <span class="status-dot"></span>
          Saved for this browser
        </p>

        <dl class="detail-list detail-list--settings">
          <div>
            <dt>Current backend</dt>
            <dd>{{ settings.apiBaseUrl }}</dd>
          </div>
          <div>
            <dt>Demo accounts</dt>
            <dd>admin / admin123, user / user123</dd>
          </div>
        </dl>
      </PanelCard>
    </div>

    <div class="page-grid two-col monitoring-card-row">
      <PanelCard
        eyebrow="Backend"
        title="Operational links"
        description="Open health, info, metrics, and Prometheus views for the current API."
      >
        <div class="list">
          <div v-for="link in backendLinks" :key="link.label" class="list-item monitoring-link">
            <div class="section-row">
              <div>
                <strong>{{ link.label }}</strong>
                <p>{{ link.description }}</p>
              </div>
              <a :href="link.href" target="_blank" rel="noreferrer">
                <Button label="Open" icon="pi pi-external-link" severity="secondary" />
              </a>
            </div>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        v-if="monitoringLinks.showLocalStack"
        eyebrow="Local stack"
        title="Prometheus and Grafana"
        description="When the local Docker stack is running, these tools show metrics and dashboards."
      >
        <div class="list">
          <div v-for="link in localStackLinks" :key="link.label" class="list-item monitoring-link">
            <div class="section-row">
              <div>
                <strong>{{ link.label }}</strong>
                <p>{{ link.description }}</p>
              </div>
              <a :href="link.href" target="_blank" rel="noreferrer">
                <Button label="Open" icon="pi pi-external-link" severity="secondary" />
              </a>
            </div>
          </div>
        </div>
      </PanelCard>
    </div>
  </section>
</template>
