<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { countryCodeLabel, countryFlagUrl } from '@/shared/utils/countries';
import { loadSettings } from '@/shared/services/settings';
import {
  getAnalyticsDetails,
  getAnalyticsSummary,
} from '@/features/links/repositories/link.repository';
import type {
  AnalyticsBucketStat,
  AnalyticsDetailsResponse,
  AnalyticsSummaryResponse,
} from '@/shared/types/api';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';

const route = useRoute();
const settings = loadSettings();

const summary = ref<AnalyticsSummaryResponse | null>(null);
const details = ref<AnalyticsDetailsResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const analyticsMessage = ref('');

const selectedCode = computed(() => String(route.params.code ?? ''));

const topCountryBars = computed(() => {
  const countries = summary.value?.topCountries ?? [];
  return countries.map((item) => ({
    country: item.country,
    code: countryCodeLabel(item.country),
    flagUrl: countryFlagUrl(item.country),
    clicks: item.clicks,
  }));
});

const sourceBreakdown = computed(() => {
  if (!summary.value) {
    return [];
  }

  return [
    {
      label: 'Redirect clicks',
      value: summary.value.redirectClicks,
    },
    {
      label: 'QR scans',
      value: summary.value.qrScans,
    },
  ];
});

const timelineByDay = computed(() => details.value?.timelineByDay ?? []);
const timelineByHour = computed(() => details.value?.timelineByHour.slice(-8) ?? []);
const browserBreakdown = computed(() => details.value?.browserBreakdown ?? []);
const deviceBreakdown = computed(() => details.value?.deviceBreakdown ?? []);
const referrerBreakdown = computed(() => details.value?.referrerBreakdown ?? []);
const sourceTrendByDay = computed(() => details.value?.sourceTrendByDay ?? []);
const visitorTrendByDay = computed(() => details.value?.visitorTrendByDay ?? []);
const recentEvents = computed(() => details.value?.recentEvents ?? []);

function chartWidthPercent(value: number, max: number, minimum = 6) {
  if (value <= 0 || max <= 0) {
    return 0;
  }

  return Math.max(minimum, Math.round((value / max) * 100));
}

const timelineDayChart = computed(() => {
  const buckets = timelineByDay.value;
  const max = Math.max(...buckets.map((bucket) => bucket.totalClicks), 1);
  return buckets.map((bucket) => ({
    ...bucket,
    width: chartWidthPercent(bucket.totalClicks, max),
  }));
});

const countryChart = computed(() => {
  const max = Math.max(...topCountryBars.value.map((item) => item.clicks), 1);
  return topCountryBars.value.map((item) => ({
    ...item,
    width: chartWidthPercent(item.clicks, max),
  }));
});

const sourceTrendChart = computed(() =>
  sourceTrendByDay.value.map((bucket) => {
    const total = Math.max(bucket.redirectClicks + bucket.qrScans, 1);
    return {
      ...bucket,
      redirectWidth: Math.round((bucket.redirectClicks / total) * 100),
      qrWidth: Math.round((bucket.qrScans / total) * 100),
    };
  }),
);

const visitorTrendChart = computed(() =>
  visitorTrendByDay.value.map((bucket) => {
    const returning = returningVisitors(bucket);
    const total = Math.max(bucket.uniqueVisitors + returning, 1);
    return {
      ...bucket,
      returning,
      uniqueWidth: Math.round((bucket.uniqueVisitors / total) * 100),
      returningWidth: Math.round((returning / total) * 100),
    };
  }),
);

function breakdownChart(items: { label: string; clicks: number }[]) {
  const max = Math.max(...items.map((item) => item.clicks), 1);
  return items.map((item) => ({
    ...item,
    width: chartWidthPercent(item.clicks, max),
  }));
}

const browserChart = computed(() => breakdownChart(browserBreakdown.value));
const deviceChart = computed(() => breakdownChart(deviceBreakdown.value));
const referrerChart = computed(() => breakdownChart(referrerBreakdown.value));

const visitorReturnRate = computed(() => {
  if (!summary.value || summary.value.uniqueVisitors === 0) {
    return '0%';
  }

  const repeatSignals = Math.max(summary.value.totalClicks - summary.value.uniqueVisitors, 0);
  return `${Math.round((repeatSignals / summary.value.totalClicks) * 100)}%`;
});

const dominantCountry = computed(() => {
  const topCountry = summary.value?.topCountries[0];
  return topCountry ? countryCodeLabel(topCountry.country) : 'No country data yet';
});

async function load(codeValue: string) {
  const trimmed = codeValue.trim();
  if (!trimmed) {
    errorMessage.value = 'Open analytics from a link row to load a short code.';
    summary.value = null;
    details.value = null;
    analyticsMessage.value = '';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  analyticsMessage.value = '';

  try {
    const [summaryResponse, detailsResponse] = await Promise.all([
      getAnalyticsSummary(trimmed, settings),
      getAnalyticsDetails(trimmed, settings),
    ]);
    summary.value = summaryResponse;
    details.value = detailsResponse;
  } catch (error) {
    summary.value = null;
    details.value = null;
    if (error instanceof ApiRequestError && error.status === 403) {
      analyticsMessage.value = 'Analytics are available only to the link owner or an admin user.';
    } else if (error instanceof ApiRequestError && error.status === 401) {
      analyticsMessage.value = 'Sign in to view analytics for this link.';
    } else {
      errorMessage.value = error instanceof Error ? error.message : 'Could not load analytics';
    }
  } finally {
    loading.value = false;
  }
}

function returningVisitors(bucket: AnalyticsBucketStat) {
  return Math.max(bucket.totalClicks - bucket.uniqueVisitors, 0);
}

function formatEventSource(value: string) {
  return value === 'QR_SCAN' ? 'QR scan' : 'Redirect';
}

function formatDate(value: string | null) {
  if (!value) {
    return 'Not available';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(() => {
  load(selectedCode.value);
});

watch(
  () => route.params.code,
  (value) => {
    if (typeof value === 'string' && value) {
      load(value);
    }
  },
);
</script>

<template>
  <section class="page-grid analytics-detail-page">
    <PageIntro
      eyebrow="Analytics"
      :title="`Analytics for &quot;${selectedCode}&quot;`"
      description="Inspect how this link is used across redirects, QR scans, visitors, countries, and recent activity."
    >
      <template #actions>
        <RefreshButton v-if="summary" :loading="loading" @refresh="load(selectedCode)" />
      </template>
    </PageIntro>

    <p v-if="errorMessage" class="status error">
      <span class="status-dot"></span>
      {{ errorMessage }}
    </p>

    <p v-if="analyticsMessage" class="status warning">
      <span class="status-dot"></span>
      {{ analyticsMessage }}
    </p>

    <template v-if="summary">
      <div class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Overview"
          title="What happened"
          description="A quick count of redirects, QR scans, and unique visitor signals."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div>
              <dt>Total interactions</dt>
              <dd>{{ summary.totalClicks }}</dd>
            </div>
            <div>
              <dt>Redirect clicks</dt>
              <dd>{{ summary.redirectClicks }}</dd>
            </div>
            <div>
              <dt>QR scans</dt>
              <dd>{{ summary.qrScans }}</dd>
            </div>
            <div>
              <dt>Unique visitors</dt>
              <dd>{{ summary.uniqueVisitors }}</dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Last interaction"
          title="Latest activity"
          description="The most recent captured visit context for this link."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div>
              <dt>Time</dt>
              <dd>{{ formatDate(summary.lastClickedAt) }}</dd>
            </div>
            <div>
              <dt>Referrer</dt>
              <dd>{{ summary.lastReferrer ?? 'No referrer captured yet' }}</dd>
            </div>
            <div>
              <dt>Browser</dt>
              <dd>{{ summary.lastBrowserFamily ?? 'Unknown browser' }}</dd>
            </div>
            <div>
              <dt>Device</dt>
              <dd>{{ summary.lastDeviceType ?? 'Unknown device' }}</dd>
            </div>
          </dl>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Traffic mix"
          title="Traffic channels"
          description="Shows whether people opened the short URL directly or scanned the QR code."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="source in sourceBreakdown" :key="source.label">
              <dt>{{ source.label }}</dt>
              <dd>{{ source.value }}</dd>
            </div>
            <div>
              <dt>Repeat signal</dt>
              <dd>{{ visitorReturnRate }}</dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Trend"
          title="QR vs redirect trend"
          description="Daily split between direct opens and QR scans."
        >
          <div class="analytics-chart" aria-label="QR versus redirect trend chart">
            <div
              v-for="bucket in sourceTrendChart"
              :key="bucket.bucket"
              class="analytics-chart-row"
            >
              <div class="analytics-chart-row__meta">
                <span>{{ bucket.bucket }}</span>
                <strong>{{ bucket.totalClicks }}</strong>
              </div>
              <span class="analytics-chart-track analytics-chart-track--stacked">
                <span
                  class="analytics-chart-fill analytics-chart-fill--redirect"
                  :style="{ width: `${bucket.redirectWidth}%` }"
                ></span>
                <span
                  class="analytics-chart-fill analytics-chart-fill--qr"
                  :style="{ width: `${bucket.qrWidth}%` }"
                ></span>
              </span>
              <span class="analytics-chart-legend">
                Redirects: {{ bucket.redirectClicks }} · QR: {{ bucket.qrScans }}
              </span>
            </div>
          </div>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Timeline"
          title="Daily timeline"
          description="Total interactions grouped by day."
        >
          <div v-if="timelineDayChart.length" class="analytics-chart" aria-label="Timeline chart">
            <div
              v-for="bucket in timelineDayChart"
              :key="`chart-day-${bucket.bucket}`"
              class="analytics-chart-row"
            >
              <div class="analytics-chart-row__meta">
                <span>{{ bucket.bucket }}</span>
                <strong>{{ bucket.totalClicks }}</strong>
              </div>
              <span class="analytics-chart-track">
                <span class="analytics-chart-fill" :style="{ width: `${bucket.width}%` }"></span>
              </span>
            </div>
          </div>
          <p v-else class="muted">No daily timeline data has been captured yet.</p>
        </PanelCard>

        <PanelCard
          eyebrow="Timeline"
          title="Hourly activity"
          description="Latest hourly buckets captured for this link."
        >
          <dl
            v-if="timelineByHour.length"
            class="detail-list detail-list--analytics analytics-detail-list"
          >
            <div v-for="bucket in timelineByHour" :key="`hour-${bucket.bucket}`">
              <dt>{{ bucket.bucket }}</dt>
              <dd>{{ bucket.totalClicks }}</dd>
            </div>
          </dl>
          <p v-else class="muted">No hourly activity has been captured yet.</p>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Visitors"
          title="Unique vs returning visitors"
          description="Daily estimate of first-time and repeat activity."
        >
          <div class="analytics-chart" aria-label="Unique versus returning visitors chart">
            <div
              v-for="bucket in visitorTrendChart"
              :key="bucket.bucket"
              class="analytics-chart-row"
            >
              <div class="analytics-chart-row__meta">
                <span>{{ bucket.bucket }}</span>
                <strong>{{ bucket.totalClicks }}</strong>
              </div>
              <span class="analytics-chart-track analytics-chart-track--stacked">
                <span
                  class="analytics-chart-fill analytics-chart-fill--unique"
                  :style="{ width: `${bucket.uniqueWidth}%` }"
                ></span>
                <span
                  class="analytics-chart-fill analytics-chart-fill--returning"
                  :style="{ width: `${bucket.returningWidth}%` }"
                ></span>
              </span>
              <span class="analytics-chart-legend">
                Unique: {{ bucket.uniqueVisitors }} · Returning: {{ bucket.returning }}
              </span>
            </div>
          </div>
        </PanelCard>

        <PanelCard
          eyebrow="Countries"
          title="Country distribution"
          :description="`Dominant country: ${dominantCountry}`"
        >
          <div v-if="countryChart.length" class="analytics-chart" aria-label="Country chart">
            <div
              v-for="bar in countryChart"
              :key="`chart-${bar.country}`"
              class="analytics-chart-row"
            >
              <div class="analytics-chart-row__meta">
                <span>
                  <img
                    v-if="bar.flagUrl"
                    class="country-flag"
                    :src="bar.flagUrl"
                    :alt="`${bar.code} flag`"
                  />
                  {{ bar.code }}
                </span>
                <strong>{{ bar.clicks }}</strong>
              </div>
              <span class="analytics-chart-track">
                <span class="analytics-chart-fill" :style="{ width: `${bar.width}%` }"></span>
              </span>
            </div>
          </div>
          <p v-else class="muted">No country data has been captured yet.</p>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid analytics-detail-row analytics-detail-row--three">
        <PanelCard
          eyebrow="Browsers"
          title="Browser breakdown"
          description="Which browser families appear most often in captured visits."
        >
          <div class="analytics-chart" aria-label="Browser breakdown chart">
            <div v-for="item in browserChart" :key="item.label" class="analytics-chart-row">
              <div class="analytics-chart-row__meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.clicks }}</strong>
              </div>
              <span class="analytics-chart-track">
                <span class="analytics-chart-fill" :style="{ width: `${item.width}%` }"></span>
              </span>
            </div>
          </div>
        </PanelCard>

        <PanelCard
          eyebrow="Devices"
          title="Device breakdown"
          description="How activity is split across desktop, mobile, and other devices."
        >
          <div class="analytics-chart" aria-label="Device breakdown chart">
            <div v-for="item in deviceChart" :key="item.label" class="analytics-chart-row">
              <div class="analytics-chart-row__meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.clicks }}</strong>
              </div>
              <span class="analytics-chart-track">
                <span class="analytics-chart-fill" :style="{ width: `${item.width}%` }"></span>
              </span>
            </div>
          </div>
        </PanelCard>

        <PanelCard
          eyebrow="Referrers"
          title="Referrer breakdown"
          description="Where visitors came from when a referrer is available."
        >
          <div class="analytics-chart" aria-label="Referrer breakdown chart">
            <div v-for="item in referrerChart" :key="item.label" class="analytics-chart-row">
              <div class="analytics-chart-row__meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.clicks }}</strong>
              </div>
              <span class="analytics-chart-track">
                <span class="analytics-chart-fill" :style="{ width: `${item.width}%` }"></span>
              </span>
            </div>
          </div>
        </PanelCard>
      </div>

      <PanelCard
        v-if="details"
        eyebrow="Recent events"
        title="Recent interactions"
        description="The latest redirect and QR scan events captured for this link."
      >
        <dl
          v-if="recentEvents.length"
          class="detail-list detail-list--analytics analytics-detail-list"
        >
          <div v-for="event in recentEvents" :key="`${event.clickedAt}-${event.eventSource}`">
            <dt>{{ formatDate(event.clickedAt) }}</dt>
            <dd>
              {{ formatEventSource(event.eventSource) }} · {{ event.country ?? 'UNKNOWN' }} ·
              {{ event.browserFamily ?? 'UNKNOWN' }} ·
              {{ event.deviceType ?? 'UNKNOWN' }}
            </dd>
          </div>
        </dl>
        <p v-else class="muted">No interaction events have been captured yet.</p>
      </PanelCard>
    </template>
  </section>
</template>
