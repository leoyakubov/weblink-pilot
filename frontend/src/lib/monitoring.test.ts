import { describe, expect, it } from 'vitest';
import { buildMonitoringLinks } from './monitoring';

describe('buildMonitoringLinks', () => {
  it('builds same-origin actuator links for the frontend proxy', () => {
    const links = buildMonitoringLinks({
      apiBaseUrl: '/api/v1/',
      authToken: '',
      refreshToken: '',
    });

    expect(links).toEqual({
      backendHealthUrl: '/actuator/health',
      backendInfoUrl: '/actuator/info',
      backendMetricsUrl: '/actuator/metrics',
      backendPrometheusUrl: '/actuator/prometheus',
      prometheusUrl: 'http://localhost:9090',
      grafanaUrl: 'http://localhost:3001',
      showLocalStack: true,
    });
  });

  it('builds localhost actuator links and shows the local stack', () => {
    const links = buildMonitoringLinks({
      apiBaseUrl: 'http://localhost:8080/api/v1/',
      authToken: '',
      refreshToken: '',
    });

    expect(links.backendHealthUrl).toBe('http://localhost:8080/actuator/health');
    expect(links.backendInfoUrl).toBe('http://localhost:8080/actuator/info');
    expect(links.backendMetricsUrl).toBe('http://localhost:8080/actuator/metrics');
    expect(links.backendPrometheusUrl).toBe('http://localhost:8080/actuator/prometheus');
    expect(links.showLocalStack).toBe(true);
    expect(links.prometheusUrl).toBe('http://localhost:9090');
    expect(links.grafanaUrl).toBe('http://localhost:3001');
  });

  it('builds remote actuator links without local stack links', () => {
    const links = buildMonitoringLinks({
      apiBaseUrl: 'https://weblink-pilot.onrender.com/api/v1',
      authToken: '',
      refreshToken: '',
    });

    expect(links.backendHealthUrl).toBe('https://weblink-pilot.onrender.com/actuator/health');
    expect(links.backendInfoUrl).toBe('https://weblink-pilot.onrender.com/actuator/info');
    expect(links.backendMetricsUrl).toBe('https://weblink-pilot.onrender.com/actuator/metrics');
    expect(links.backendPrometheusUrl).toBe(
      'https://weblink-pilot.onrender.com/actuator/prometheus',
    );
    expect(links.showLocalStack).toBe(false);
    expect(links.prometheusUrl).toBe('');
    expect(links.grafanaUrl).toBe('');
  });
});
