import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getAdminMonitoring } from '@/admin/monitoring/MonitoringApi';
import { buildMonitoringLinks } from '@/admin/monitoring/MonitoringLinks';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import type {
  AdminMonitoringResponse,
  AdminRuntimeMetricResponse,
  ApiSettings,
} from '@/shared/types/api';

export function useMonitoringView() {
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
      errorMessage.value =
        error instanceof Error ? error.message : 'Could not load monitoring data';
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
  return {
    settings,
    canEditBackendUrl,
    monitoringLinks,
    frontendUrl,
    monitoring,
    loading,
    errorMessage,
    saved,
    runtimeLabel,
    authStateLabel,
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
  };
}
