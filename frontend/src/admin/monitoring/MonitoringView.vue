<script setup lang="ts">
import './MonitoringView.css';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import PageIntro from '@/shared/components/PageIntro.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import RefreshButton from '@/shared/components/RefreshButton.vue';
import { useMonitoringView } from './MonitoringView';

const {
  settings,
  canEditBackendUrl,
  loading,
  errorMessage,
  saved,
  runtimeMetrics,
  healthChecks,
  configurationItems,
  groupedRuntimeMetrics,
  runtimeSummary,
  endpointLinks,
  refresh,
  saveBaseUrl,
  resetBrowserSettings,
  formatMetricDescription,
  healthStatusClass,
} = useMonitoringView();
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
