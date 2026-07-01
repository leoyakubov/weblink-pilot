import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { countryCodeLabel, countryFlagUrl } from '@/shared/utils/countries';
import { loadSettings } from '@/shared/services/settings';
import { getAnalyticsDetails, getAnalyticsSummary } from '@/features/analytics/AnalyticsApi';
import type {
  AnalyticsBucketStat,
  AnalyticsDetailsResponse,
  AnalyticsSummaryResponse,
} from '@/shared/types/api';

export function useAnalyticsDetailView() {
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
  return {
    summary,
    details,
    loading,
    errorMessage,
    analyticsMessage,
    selectedCode,
    topCountryBars,
    sourceBreakdown,
    timelineByDay,
    timelineByHour,
    browserBreakdown,
    deviceBreakdown,
    referrerBreakdown,
    sourceTrendByDay,
    visitorTrendByDay,
    recentEvents,
    timelineDayChart,
    countryChart,
    sourceTrendChart,
    visitorTrendChart,
    browserChart,
    deviceChart,
    referrerChart,
    visitorReturnRate,
    dominantCountry,
    load,
    returningVisitors,
    formatEventSource,
    formatDate,
  };
}
