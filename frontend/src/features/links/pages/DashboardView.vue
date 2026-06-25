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
  AnalyticsBreakdownStat,
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

const chartWidth = 320;
const chartHeight = 132;

const topCountryBars = computed(() => {
  const countries = summary.value?.topCountries ?? [];
  const max = Math.max(...countries.map((item) => item.clicks), 1);
  return countries.map((item) => ({
    country: item.country,
    code: countryCodeLabel(item.country),
    flagUrl: countryFlagUrl(item.country),
    clicks: item.clicks,
    width: Math.max(8, Math.round((item.clicks / max) * 100)),
  }));
});

const sourceBreakdown = computed(() => {
  if (!summary.value) {
    return [];
  }

  const total = Math.max(summary.value.totalClicks, 1);
  return [
    {
      label: 'Redirect clicks',
      value: summary.value.redirectClicks,
      width: Math.round((summary.value.redirectClicks / total) * 100),
    },
    {
      label: 'QR scans',
      value: summary.value.qrScans,
      width: Math.round((summary.value.qrScans / total) * 100),
    },
  ];
});

const timelineByDay = computed(() => details.value?.timelineByDay ?? []);
const timelineByHour = computed(() => details.value?.timelineByHour.slice(-8) ?? []);
const browserBreakdown = computed(() => withBreakdownWidths(details.value?.browserBreakdown ?? []));
const deviceBreakdown = computed(() => withBreakdownWidths(details.value?.deviceBreakdown ?? []));
const referrerBreakdown = computed(() =>
  withBreakdownWidths(details.value?.referrerBreakdown ?? []),
);
const sourceTrendByDay = computed(() => details.value?.sourceTrendByDay ?? []);
const visitorTrendByDay = computed(() => details.value?.visitorTrendByDay ?? []);
const recentEvents = computed(() => details.value?.recentEvents ?? []);

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

const sparklinePoints = computed(() => {
  const values = summary.value?.topCountries.map((item) => item.clicks) ?? [];
  if (!values.length) {
    return '0,100 100,100';
  }

  const max = Math.max(...values, 1);
  const step = values.length === 1 ? chartWidth / 2 : chartWidth / (values.length - 1);
  return values
    .map((value, index) => {
      const x = Math.round(index * step);
      const y = Math.round(chartHeight - 24 - (value / max) * (chartHeight - 36));
      return `${x},${y}`;
    })
    .join(' ');
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

function withBreakdownWidths(items: AnalyticsBreakdownStat[]) {
  const max = Math.max(...items.map((item) => item.clicks), 1);
  return items.map((item) => ({
    ...item,
    width: Math.max(8, Math.round((item.clicks / max) * 100)),
  }));
}

function bucketWidth(
  bucket: AnalyticsBucketStat,
  buckets: AnalyticsBucketStat[],
  key: 'totalClicks' | 'redirectClicks' | 'qrScans' | 'uniqueVisitors',
) {
  const max = Math.max(...buckets.map((item) => item[key]), 1);
  return Math.max(8, Math.round((bucket[key] / max) * 100));
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
  <section class="page-grid">
    <PageIntro
      eyebrow="Analytics"
      :title="`Analytics for &quot;${selectedCode}&quot;`"
      description="Inspect traffic sources, visitor signals, last interaction context, and country distribution for this short code."
    />

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
          title="Interaction summary"
          description="High-level counters from the analytics summary endpoint."
        >
          <template #actions>
            <RefreshButton :loading="loading" @refresh="load(selectedCode)" />
          </template>

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
          title="Latest captured context"
          description="The most recent analytics event metadata currently available from the backend."
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

      <div class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Traffic mix"
          title="Redirects vs QR scans"
          description="Shows how people reached the target URL: direct redirect route or QR scan route."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="source in sourceBreakdown" :key="source.label">
              <dt>{{ source.label }}</dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ source.value }}</strong>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${source.width}%` }"></span>
                  </span>
                </span>
              </dd>
            </div>
            <div>
              <dt>Repeat signal</dt>
              <dd>{{ visitorReturnRate }}</dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Countries"
          title="Country distribution"
          :description="`Dominant country: ${dominantCountry}`"
        >
          <dl
            v-if="topCountryBars.length"
            class="detail-list detail-list--analytics analytics-detail-list"
          >
            <div v-for="bar in topCountryBars" :key="bar.country">
              <dt>
                <span class="country-label">
                  <img
                    v-if="bar.flagUrl"
                    class="country-flag"
                    :src="bar.flagUrl"
                    :alt="`${bar.code} flag`"
                  />
                  <span>{{ bar.code }}</span>
                </span>
              </dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ bar.clicks }}</strong>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${bar.width}%` }"></span>
                  </span>
                </span>
              </dd>
            </div>
          </dl>
          <p v-else class="muted">No country data has been captured yet.</p>

          <svg
            class="sparkline"
            :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <defs>
              <linearGradient id="sparklineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#38bdf8" />
                <stop offset="100%" stop-color="#f59e0b" />
              </linearGradient>
            </defs>
            <polyline
              :points="sparklinePoints"
              fill="none"
              stroke="url(#sparklineGradient)"
              stroke-width="4"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Timeline"
          title="Timeline by day/hour"
          description="Daily totals plus the latest hourly buckets returned from backend click events."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="bucket in timelineByDay" :key="`day-${bucket.bucket}`">
              <dt>{{ bucket.bucket }}</dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ bucket.totalClicks }}</strong>
                  <span class="bar-track">
                    <span
                      class="bar-fill"
                      :style="{ width: `${bucketWidth(bucket, timelineByDay, 'totalClicks')}%` }"
                    ></span>
                  </span>
                </span>
              </dd>
            </div>
            <div v-for="bucket in timelineByHour" :key="`hour-${bucket.bucket}`">
              <dt>{{ bucket.bucket }}</dt>
              <dd>{{ bucket.totalClicks }}</dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Trend"
          title="QR vs redirect trend"
          description="Daily split between regular redirects and QR scan redirects."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="bucket in sourceTrendByDay" :key="bucket.bucket">
              <dt>{{ bucket.bucket }}</dt>
              <dd>
                <span class="analytics-trend-value">
                  <span>Redirects: {{ bucket.redirectClicks }}</span>
                  <span>QR: {{ bucket.qrScans }}</span>
                </span>
              </dd>
            </div>
          </dl>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Visitors"
          title="Unique vs returning visitors"
          description="Daily unique visitor estimate based on distinct IP addresses."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="bucket in visitorTrendByDay" :key="bucket.bucket">
              <dt>{{ bucket.bucket }}</dt>
              <dd>
                <span class="analytics-trend-value">
                  <span>Unique: {{ bucket.uniqueVisitors }}</span>
                  <span>Returning: {{ returningVisitors(bucket) }}</span>
                </span>
              </dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Browsers"
          title="Browser breakdown"
          description="Browser family parsed from captured User-Agent headers."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="item in browserBreakdown" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ item.clicks }}</strong>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${item.width}%` }"></span>
                  </span>
                </span>
              </dd>
            </div>
          </dl>
        </PanelCard>
      </div>

      <div v-if="details" class="page-grid two-col analytics-detail-row">
        <PanelCard
          eyebrow="Devices"
          title="Device breakdown"
          description="Device type parsed from the same User-Agent metadata."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="item in deviceBreakdown" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ item.clicks }}</strong>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${item.width}%` }"></span>
                  </span>
                </span>
              </dd>
            </div>
          </dl>
        </PanelCard>

        <PanelCard
          eyebrow="Referrers"
          title="Referrer breakdown"
          description="Where traffic came from, grouped by referrer host when available."
        >
          <dl class="detail-list detail-list--analytics analytics-detail-list">
            <div v-for="item in referrerBreakdown" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>
                <span class="analytics-bar-value">
                  <strong>{{ item.clicks }}</strong>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${item.width}%` }"></span>
                  </span>
                </span>
              </dd>
            </div>
          </dl>
        </PanelCard>
      </div>

      <PanelCard
        v-if="details"
        eyebrow="Recent events"
        title="Recent interactions"
        description="The latest captured redirect and QR scan events for this short code."
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
