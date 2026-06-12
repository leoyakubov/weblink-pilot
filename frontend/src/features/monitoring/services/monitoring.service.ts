import type { ApiSettings } from '@/shared/types/api';
import { normalizeBaseUrl } from '@/shared/services/settings';

export interface MonitoringLinks {
  backendHealthUrl: string;
  backendInfoUrl: string;
  backendMetricsUrl: string;
  backendPrometheusUrl: string;
  prometheusUrl: string;
  grafanaUrl: string;
  showLocalStack: boolean;
}

function buildBackendRoot(apiBaseUrl: string) {
  const normalized = normalizeBaseUrl(apiBaseUrl);
  const apiSuffix = '/api/v1';

  if (normalized.endsWith(apiSuffix)) {
    const root = normalized.slice(0, normalized.length - apiSuffix.length);
    return root || '';
  }

  return normalized;
}

function isLocalOrigin(baseUrl: string) {
  return (
    baseUrl.startsWith('/') ||
    /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?/i.test(baseUrl) ||
    /:\/\/[^/]*\.local(?::\d+)?$/i.test(baseUrl)
  );
}

export function buildMonitoringLinks(settings: ApiSettings): MonitoringLinks {
  const backendRoot = buildBackendRoot(settings.apiBaseUrl);
  const backendPrefix = backendRoot ? `${backendRoot}/actuator` : '/actuator';
  const showLocalStack = isLocalOrigin(normalizeBaseUrl(settings.apiBaseUrl));

  return {
    backendHealthUrl: `${backendPrefix}/health`,
    backendInfoUrl: `${backendPrefix}/info`,
    backendMetricsUrl: `${backendPrefix}/metrics`,
    backendPrometheusUrl: `${backendPrefix}/prometheus`,
    prometheusUrl: showLocalStack ? 'http://localhost:9090' : '',
    grafanaUrl: showLocalStack ? 'http://localhost:3001' : '',
    showLocalStack,
  };
}
