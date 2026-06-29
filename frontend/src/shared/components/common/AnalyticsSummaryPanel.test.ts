import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AnalyticsSummaryPanel from '@/shared/components/common/AnalyticsSummaryPanel.vue';
import type { AnalyticsSummaryResponse } from '@/shared/types/api';

const summary: AnalyticsSummaryResponse = {
  code: 'redis',
  totalClicks: 8,
  redirectClicks: 6,
  qrScans: 2,
  uniqueVisitors: 5,
  lastClickedAt: '2026-06-27T07:08:00Z',
  lastReferrer: 'https://github.com',
  lastBrowserFamily: 'Chrome',
  lastDeviceType: 'Desktop',
  topCountries: [
    { country: 'US', clicks: 4 },
    { country: 'DE', clicks: 2 },
  ],
};

describe('AnalyticsSummaryPanel', () => {
  it('renders metrics and top countries when summary is available', () => {
    const wrapper = mount(AnalyticsSummaryPanel, {
      props: { summary, eyebrow: 'Countries', title: 'Country split' },
    });

    expect(wrapper.text()).toContain('8');
    expect(wrapper.text()).toContain('Total interactions');
    expect(wrapper.text()).toContain('Redirect clicks');
    expect(wrapper.text()).toContain('QR scans');
    expect(wrapper.text()).toContain('https://github.com');
    expect(wrapper.text()).toContain('Country split');
    expect(wrapper.text()).toContain('US');
    expect(wrapper.text()).toContain('DE');
  });

  it('can hide metric and country sections independently', () => {
    const wrapper = mount(AnalyticsSummaryPanel, {
      props: { summary, showMetrics: false, showTopCountries: false },
    });

    expect(wrapper.text()).toBe('');
  });
});
