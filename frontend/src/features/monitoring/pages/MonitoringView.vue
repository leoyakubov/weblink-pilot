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
    description: 'Health endpoint and readiness state.',
    href: monitoringLinks.backendHealthUrl,
  },
  {
    label: 'Info',
    description: 'Build and application info endpoint.',
    href: monitoringLinks.backendInfoUrl,
  },
  {
    label: 'Metrics',
    description: 'Raw Spring Boot metrics namespace.',
    href: monitoringLinks.backendMetricsUrl,
  },
  {
    label: 'Prometheus',
    description: 'Prometheus scrape endpoint for the backend.',
    href: monitoringLinks.backendPrometheusUrl,
  },
]);
const localStackLinks = computed(() =>
  monitoringLinks.showLocalStack
    ? [
        {
          label: 'Prometheus',
          description: 'Local scrape and query UI for Docker dev.',
          href: monitoringLinks.prometheusUrl,
        },
        {
          label: 'Grafana',
          description: 'Local dashboards for the Docker monitoring stack.',
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
      description="Review system totals, open operational endpoints, and adjust local browser/backend settings from one place."
    />

    <div class="page-grid two-col">
      <PanelCard
        eyebrow="Admin overview"
        title="System overview"
        description="User totals and link ownership counts show how the app is being used."
      >
        <template #actions>
          <RefreshButton :loading="loading" @refresh="refresh" />
        </template>

        <p class="help-text">
          This page is for admins only. It combines user totals and link ownership counts so you can
          see how the app is being used.
        </p>

        <p v-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>

        <div v-if="overview" class="grid-2">
          <div class="metric">
            <span class="label">Total users</span>
            <span class="value">{{ overview.totalUsers }}</span>
          </div>
          <div class="metric">
            <span class="label">Admin users</span>
            <span class="value">{{ overview.adminUsers }}</span>
          </div>
          <div class="metric">
            <span class="label">Total links</span>
            <span class="value">{{ overview.totalLinks }}</span>
          </div>
          <div class="metric">
            <span class="label">Total clicks</span>
            <span class="value">{{ overview.totalClicks }}</span>
          </div>
          <div class="metric">
            <span class="label">Owned links</span>
            <span class="value">{{ overview.ownedLinks }}</span>
          </div>
          <div class="metric">
            <span class="label">Anonymous links</span>
            <span class="value">{{ overview.anonymousLinks }}</span>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Live backend"
        title="Actuator endpoints"
        description="Open backend health, info, metrics, and scrape endpoints for the current environment."
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
    </div>

    <div class="page-grid two-col">
      <PanelCard
        eyebrow="Frontend settings"
        title="Backend base URL"
        description="Use these browser-local settings when testing a different backend endpoint."
      >
        <div class="section-row about-toolbar">
          <div class="about-toolbar__copy">
            <p class="eyebrow about-toolbar__eyebrow">Browser tools</p>
            <p class="help-text">
              Clear saved browser state when the demo points at stale settings.
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

        <dl class="detail-list">
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

      <PanelCard
        v-if="monitoringLinks.showLocalStack"
        eyebrow="Local stack"
        title="Prometheus and Grafana"
        description="When you start the Docker full stack, Prometheus scrapes the backend and Grafana reads from Prometheus without extra setup."
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
