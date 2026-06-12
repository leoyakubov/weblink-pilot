<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import Button from 'primevue/button';
import { getAdminOverview } from '@/features/monitoring/repositories/monitoring.repository';
import { buildMonitoringLinks } from '@/features/monitoring/services/monitoring.service';
import { loadSettings } from '@/shared/services/settings';
import type { AdminOverviewResponse } from '@/shared/types/api';

const settings = loadSettings();
const monitoringLinks = buildMonitoringLinks(settings);
const overview = ref<AdminOverviewResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
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

onMounted(() => {
  refresh();
});
</script>

<template>
  <section class="page-grid two-col">
    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Admin monitoring</p>
            <h3 class="panel-title">System overview and ownership mix.</h3>
          </div>
          <Button
            type="button"
            :label="loading ? 'Refreshing...' : 'Refresh'"
            icon="pi pi-refresh"
            severity="secondary"
            variant="outlined"
            :disabled="loading"
            @click="refresh"
          />
        </div>

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
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Live backend</p>
            <h3 class="panel-title">Actuator endpoints and metrics.</h3>
          </div>
        </div>

        <p class="help-text">
          These links point at the live backend actuator endpoints for the current environment. In
          the local Docker stack, the frontend proxy serves them from the same origin.
        </p>

        <div class="list">
          <div v-for="link in backendLinks" :key="link.label" class="list-item">
            <div class="section-row">
              <div>
                <strong>{{ link.label }}</strong>
                <p>{{ link.description }}</p>
              </div>
              <a :href="link.href" target="_blank" rel="noreferrer">
                <Button label="Open" severity="secondary" variant="outlined" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </article>
  </section>

  <section v-if="monitoringLinks.showLocalStack" class="page-grid">
    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Local stack</p>
            <h3 class="panel-title">Prometheus and Grafana for Docker dev.</h3>
          </div>
        </div>

        <p class="help-text">
          When you start the Docker full stack, Prometheus scrapes the backend and Grafana reads
          from Prometheus without any extra setup.
        </p>

        <div class="list">
          <div v-for="link in localStackLinks" :key="link.label" class="list-item">
            <div class="section-row">
              <div>
                <strong>{{ link.label }}</strong>
                <p>{{ link.description }}</p>
              </div>
              <a :href="link.href" target="_blank" rel="noreferrer">
                <Button label="Open" severity="secondary" variant="outlined" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>
