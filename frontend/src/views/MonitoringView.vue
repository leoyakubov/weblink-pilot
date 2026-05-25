<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getAdminOverview } from '@/lib/api';
import { loadSettings } from '@/lib/settings';
import type { AdminOverviewResponse } from '@/types';

const settings = loadSettings();
const overview = ref<AdminOverviewResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');

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
  <section class="page-grid">
    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Admin monitoring</p>
            <h3 class="panel-title">System overview and ownership mix.</h3>
          </div>
          <button
            class="button button-secondary"
            type="button"
            :disabled="loading"
            @click="refresh"
          >
            {{ loading ? 'Refreshing...' : 'Refresh' }}
          </button>
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
            <span class="value">{{ overview.totalUsers }}</span
            ><span class="label">Total users</span>
          </div>
          <div class="metric">
            <span class="value">{{ overview.adminUsers }}</span
            ><span class="label">Admin users</span>
          </div>
          <div class="metric">
            <span class="value">{{ overview.totalLinks }}</span
            ><span class="label">Total links</span>
          </div>
          <div class="metric">
            <span class="value">{{ overview.totalClicks }}</span
            ><span class="label">Total clicks</span>
          </div>
          <div class="metric">
            <span class="value">{{ overview.ownedLinks }}</span
            ><span class="label">Owned links</span>
          </div>
          <div class="metric">
            <span class="value">{{ overview.anonymousLinks }}</span
            ><span class="label">Anonymous links</span>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>
