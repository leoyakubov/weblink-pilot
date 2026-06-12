<script setup lang="ts">
import type { AnalyticsSummaryResponse } from '@/types';
import { countryCodeLabel, countryFlagUrl } from '@/shared/utils/countries';

const props = defineProps<{
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
  <div v-if="props.summary" class="stack">
    <template v-if="props.showMetrics !== false">
      <div class="grid-2">
        <div class="metric">
          <span class="value">{{ props.summary.totalClicks }}</span>
          <span class="label">Total interactions</span>
        </div>
        <div class="metric">
          <span class="value">{{ props.summary.redirectClicks }}</span>
          <span class="label">Redirect clicks</span>
        </div>
        <div class="metric">
          <span class="value">{{ props.summary.qrScans }}</span>
          <span class="label">QR scans</span>
        </div>
        <div class="metric">
          <span class="value">{{ props.summary.uniqueVisitors }}</span>
          <span class="label">Unique visitors</span>
        </div>
      </div>

      <div class="list-item">
        <strong>Last click</strong>
        <p>{{ formatDate(props.summary.lastClickedAt) }}</p>
      </div>
      <div class="list-item">
        <strong>Last referrer</strong>
        <p>{{ props.summary.lastReferrer ?? 'No referrer captured yet' }}</p>
      </div>
      <div class="list-item">
        <strong>Browser / device</strong>
        <p>
          {{ props.summary.lastBrowserFamily ?? 'Unknown browser' }} -
          {{ props.summary.lastDeviceType ?? 'Unknown device' }}
        </p>
      </div>
    </template>

    <div v-if="(props.showTopCountries !== false) && props.summary.topCountries.length" class="stack">
      <div>
        <p class="eyebrow">{{ props.eyebrow ?? 'Top countries' }}</p>
        <h4 class="card-title">{{ props.title ?? 'Where clicks are coming from' }}</h4>
      </div>
      <div class="list">
        <div v-for="country in props.summary.topCountries" :key="country.country" class="list-item">
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
