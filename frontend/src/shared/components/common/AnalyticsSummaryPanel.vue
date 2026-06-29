<script setup lang="ts">
import type { AnalyticsSummaryResponse } from '@/shared/types/api';
import { countryCodeLabel, countryFlagUrl } from '@/shared/utils/countries';

const {
  summary,
  showMetrics = true,
  showTopCountries = true,
  eyebrow = 'Top countries',
  title = 'Where clicks are coming from',
} = defineProps<{
  summary: AnalyticsSummaryResponse | null;
  showMetrics?: boolean;
  showTopCountries?: boolean;
  eyebrow?: string;
  title?: string;
}>();

function formatDate(value: string | null) {
  if (!value) {
    return 'No clicks yet';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
</script>

<template>
  <div v-if="summary" class="stack">
    <template v-if="showMetrics">
      <div class="grid-2">
        <div class="metric">
          <span class="value">{{ summary.totalClicks }}</span>
          <span class="label">Total interactions</span>
        </div>
        <div class="metric">
          <span class="value">{{ summary.redirectClicks }}</span>
          <span class="label">Redirect clicks</span>
        </div>
        <div class="metric">
          <span class="value">{{ summary.qrScans }}</span>
          <span class="label">QR scans</span>
        </div>
        <div class="metric">
          <span class="value">{{ summary.uniqueVisitors }}</span>
          <span class="label">Unique visitors</span>
        </div>
      </div>

      <div class="list-item">
        <strong>Last click</strong>
        <p>{{ formatDate(summary.lastClickedAt) }}</p>
      </div>
      <div class="list-item">
        <strong>Last referrer</strong>
        <p>{{ summary.lastReferrer ?? 'No referrer captured yet' }}</p>
      </div>
      <div class="list-item">
        <strong>Browser / device</strong>
        <p>
          {{ summary.lastBrowserFamily ?? 'Unknown browser' }} -
          {{ summary.lastDeviceType ?? 'Unknown device' }}
        </p>
      </div>
    </template>

    <div v-if="showTopCountries && summary.topCountries.length" class="stack">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h4 class="card-title">{{ title }}</h4>
      </div>
      <div class="list">
        <div v-for="country in summary.topCountries" :key="country.country" class="list-item">
          <strong class="country-label">
            <img
              v-if="countryFlagUrl(country.country)"
              class="country-flag"
              :src="countryFlagUrl(country.country) || ''"
              :alt="`${countryCodeLabel(country.country)} flag`"
            />
            <span>{{ countryCodeLabel(country.country) }}</span>
          </strong>
          <p>{{ country.clicks }} clicks</p>
        </div>
      </div>
    </div>
  </div>
</template>
