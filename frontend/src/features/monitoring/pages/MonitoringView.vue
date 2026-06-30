<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import { getAdminMonitoring } from '@/features/monitoring/repositories/monitoring.repository';
import { buildMonitoringLinks } from '@/features/monitoring/services/monitoring.service';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import type {
  AdminMonitoringResponse,
  AdminRuntimeMetricResponse,
  ApiSettings,
} from '@/shared/types/api';

const settings = reactive<ApiSettings>(loadSettings());
const canEditBackendUrl = !import.meta.env.PROD;
const router = useRouter();
const monitoringLinks = computed(() => buildMonitoringLinks(settings));
const frontendUrl = window.location.origin;
const monitoring = ref<AdminMonitoringResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const saved = ref(false);
const runtimeLabel = computed(() => {
  const apiBaseUrl = settings.apiBaseUrl.trim();
  if (apiBaseUrl.startsWith('/')) {
    return 'same-origin proxy';
  }
  if (/localhost|127\.0\.0\.1/i.test(apiBaseUrl)) {
    return 'local development';
  }
  return 'remote demo';
});
const authStateLabel = computed(() => (settings.authToken ? 'token saved' : 'no token saved'));
const runtimeMetrics = computed(() => monitoring.value?.metrics ?? []);
const healthChecks = computed(() => monitoring.value?.health ?? []);
const configurationItems = computed(() => monitoring.value?.configuration ?? []);
const groupedRuntimeMetrics = computed(() => {
  return runtimeMetrics.value.reduce<Record<string, AdminRuntimeMetricResponse[]>>(
    (groups, metric) => {
      groups[metric.group] = [...(groups[metric.group] ?? []), metric];
      return groups;
    },
    {},
  );
});
const runtimeSummary = computed(() => [
  ['Runtime mode', runtimeLabel.value],
  ['API base URL', monitoringLinks.value.backendApiUrl],
  ['Auth state', authStateLabel.value],
  ['Local stack', monitoringLinks.value.showLocalStack ? 'available' : 'remote API only'],
]);
const endpointLinks = computed(() => [
  {
    label: 'Frontend',
    description: 'Current Vue app origin.',
    href: frontendUrl,
  },
  {
    label: 'Backend API',
    description: 'Base URL used by frontend requests.',
    href: monitoringLinks.value.backendApiUrl,
  },
  {
    label: 'Swagger UI',
    description: 'Interactive OpenAPI documentation in the browser.',
    href: monitoringLinks.value.backendSwaggerUiUrl,
  },
  ...(monitoringLinks.value.showLocalStack
    ? [
        {
          label: 'Prometheus',
          description: 'Query local metrics from the Docker stack.',
          href: monitoringLinks.value.prometheusUrl,
        },
        {
          label: 'Grafana',
          description: 'Open local dashboards for the demo stack.',
          href: monitoringLinks.value.grafanaUrl,
        },
      ]
    : []),
]);

async function refresh() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const monitoringResponse = await getAdminMonitoring(settings);
    monitoring.value = monitoringResponse;
  } catch (error) {
    monitoring.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'Could not load monitoring data';
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

function lowerFirstLetter(value: string) {
  return value ? value.charAt(0).toLowerCase() + value.slice(1) : value;
}

function formatMetricDescription(value: string) {
  return lowerFirstLetter(value).replace(/\.$/, '');
}

function healthStatusClass(status: string) {
  const normalized = status.toUpperCase();
  if (['UP', 'OK', 'INFO', 'CONFIGURED'].includes(normalized)) {
    return 'success';
  }
  if (['DOWN', 'ERROR', 'FAILED'].includes(normalized)) {
    return 'error';
  }
  return 'warning';
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
      description="Operational status for the WeblinkPilot backend, runtime settings, service dependencies, and local tooling."
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="refresh" />
      </template>
    </PageIntro>

    <div class="page-grid two-col monitoring-card-row">
      <PanelCard
        eyebrow="Health"
        title="Health checks"
        description="Live readiness checks for storage, cache, auth, email, analytics, and seeded demo data."
      >
        <p v-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>

        <div class="list monitoring-health-list">
          <div v-for="check in healthChecks" :key="check.name" class="list-item monitoring-link">
            <div class="monitoring-health-row">
              <div class="monitoring-health-copy">
                <strong>{{ check.name }}</strong>
              </div>
              <span class="status" :class="healthStatusClass(check.status)">
                <span class="status-dot"></span>
                {{ check.status }}
              </span>
              <p>{{ check.detail }}</p>
              <p v-if="check.error" class="monitoring-health-error">
                {{ check.error.type }}: {{ check.error.message }}
              </p>
            </div>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Runtime metrics"
        title="JVM and service metrics"
        description="Current JVM, HTTP, cache, datasource, and WeblinkPilot service counters."
      >
        <div v-if="runtimeMetrics.length" class="monitoring-metric-groups">
          <section
            v-for="(metrics, group) in groupedRuntimeMetrics"
            :key="group"
            class="monitoring-metric-group"
          >
            <h3>{{ group }}</h3>
            <dl
              class="detail-list detail-list--link-detail detail-list--analytics monitoring-table"
            >
              <div
                v-for="metric in metrics"
                :key="`${group}-${metric.name}`"
                class="monitoring-data-row"
              >
                <dt>
                  {{ metric.name }}
                  <span class="footnote">({{ formatMetricDescription(metric.description) }})</span>
                </dt>
                <dd>
                  <strong>{{ metric.value }}</strong>
                </dd>
              </div>
            </dl>
          </section>
        </div>

        <div v-else-if="!loading" class="empty-state">
          <p class="eyebrow">No metrics</p>
          <h4 class="card-title">Runtime metrics were not returned by the backend.</h4>
        </div>
      </PanelCard>
    </div>

    <div class="page-grid two-col monitoring-card-row">
      <PanelCard
        eyebrow="Config"
        title="Configuration"
        description="Backend and browser settings that affect this admin session."
      >
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

          <div class="actions monitoring-config-actions">
            <Button
              type="button"
              label="Save settings"
              icon="pi pi-save"
              data-testid="save-settings"
              @click="saveBaseUrl"
            />
            <Button
              type="button"
              label="Reset settings"
              severity="secondary"
              icon="pi pi-undo"
              data-testid="reset-browser-settings"
              @click="resetBrowserSettings"
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

        <dl class="detail-list detail-list--settings monitoring-runtime-list">
          <div v-for="[label, value] in runtimeSummary" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
          <div v-for="item in configurationItems" :key="item.name">
            <dt>{{ item.name }}</dt>
            <dd>
              <strong>{{ item.value }}</strong>
            </dd>
          </div>
        </dl>

        <div v-if="!canEditBackendUrl" class="actions monitoring-config-actions">
          <Button
            type="button"
            label="Reset settings"
            severity="secondary"
            icon="pi pi-undo"
            data-testid="reset-browser-settings"
            @click="resetBrowserSettings"
          />
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Endpoints"
        title="Service endpoints"
        description="Quick access to the running app, API documentation, and local observability tools."
      >
        <div class="list monitoring-endpoint-grid">
          <div v-for="link in endpointLinks" :key="link.label" class="list-item monitoring-link">
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
