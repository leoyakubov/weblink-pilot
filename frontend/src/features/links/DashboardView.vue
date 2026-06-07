<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import CopyActionButton from '@/shared/components/CopyActionButton.vue';
import { isAdminUser } from '@/lib/auth';
import { ApiRequestError, buildApiBaseUrl, getAnalyticsSummary, getLink, listLinks } from '@/lib/api';
import { countryCodeLabel, countryFlagUrl } from '@/lib/countries';
import { loadSettings } from '@/lib/settings';
import type { AnalyticsSummaryResponse, LinkResponse } from '@/types';

const route = useRoute();
const router = useRouter();
const settings = loadSettings();

const form = reactive({
  code: String(route.query.code ?? ''),
});

const link = ref<LinkResponse | null>(null);
const summary = ref<AnalyticsSummaryResponse | null>(null);
const loading = ref(false);
const errorMessage = ref('');
const analyticsMessage = ref('');
const recentLinks = ref<LinkResponse[]>([]);
const qrModalUrl = ref('');
const qrModalTitle = ref('');

const selectedCode = computed(() => form.code.trim());
const hasRecent = computed(() => recentLinks.value.length > 0);
const canSeePreview = computed(() => isAdminUser());

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

function pickDefaultCode() {
  if (selectedCode.value) {
    return selectedCode.value;
  }

  if (recentLinks.value.length > 0) {
    form.code = recentLinks.value[0].code;
    return form.code;
  }

  return '';
}

async function refreshRecent() {
  recentLinks.value = await listLinks(8, settings);
  if (!selectedCode.value && recentLinks.value.length > 0) {
    form.code = recentLinks.value[0].code;
  }
}

async function load(codeValue: string) {
  const trimmed = codeValue.trim();
  if (!trimmed) {
    errorMessage.value = 'Enter a short code to load analytics.';
    link.value = null;
    summary.value = null;
    analyticsMessage.value = '';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  analyticsMessage.value = '';

  try {
    router.replace({ query: { code: trimmed } });
    link.value = await getLink(trimmed, settings);

    try {
      summary.value = await getAnalyticsSummary(trimmed, settings);
    } catch (error) {
      summary.value = null;
      if (error instanceof ApiRequestError && error.status === 403) {
        analyticsMessage.value =
          'Analytics are available only to the link owner or an admin user.';
      } else {
        analyticsMessage.value =
          error instanceof Error ? error.message : 'Could not load analytics';
      }
    }
  } catch (error) {
    link.value = null;
    summary.value = null;
    analyticsMessage.value = '';
    errorMessage.value = error instanceof Error ? error.message : 'Could not load analytics';
  } finally {
    loading.value = false;
  }
}

function loadRecent(code: string) {
  form.code = code;
  load(code);
}

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
}

function openQrModal(url: string, title: string) {
  qrModalUrl.value = url;
  qrModalTitle.value = title;
}

function closeQrModal() {
  qrModalUrl.value = '';
  qrModalTitle.value = '';
}

function formatDate(value: string | null) {
  if (!value) {
    return 'No clicks yet';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(async () => {
  await refreshRecent();
  const initialCode = pickDefaultCode();
  if (initialCode) {
    load(initialCode);
  }
});

watch(
  () => route.query.code,
  (value) => {
    if (typeof value === 'string' && value && value !== form.code) {
      form.code = value;
      load(value);
    }
  },
);
</script>

<template>
  <section class="page-grid two-col">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Analytics dashboard</p>
          <h3 class="panel-title">Inspect clicks for any short code.</h3>
        </div>

        <form class="form-grid" @submit.prevent="load(form.code)">
          <label class="form-field">
            <span class="field-label">Short code</span>
            <InputText
              v-model="form.code"
              type="text"
              placeholder="github-org"
              autocomplete="off"
              fluid
            />
          </label>

          <div class="actions">
            <Button
              type="submit"
              :label="loading ? 'Loading...' : 'Load analytics'"
              icon="pi pi-chart-line"
              :disabled="loading"
            />
            <RouterLink to="/">
              <Button
                label="Create new link"
                icon="pi pi-plus"
                severity="secondary"
                variant="outlined"
              />
            </RouterLink>
          </div>
        </form>

        <p v-if="errorMessage" class="status error">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
        <p v-else class="help-text">
          Dashboard reads the summary API and the link details endpoint, so you can inspect
          analytics and open actions from one place.
        </p>

        <div class="chart-card" v-if="summary">
          <div class="section-row">
            <div>
              <p class="eyebrow">Country distribution</p>
              <h4 class="card-title">Clicks by country</h4>
            </div>
            <span class="badge"
              ><strong>{{ summary.totalClicks }}</strong> total interactions</span
            >
          </div>

          <div class="bar-chart" v-if="topCountryBars.length">
            <div v-for="bar in topCountryBars" :key="bar.country" class="bar-row">
              <div class="bar-labels">
                <strong class="country-label">
                  <img
                    v-if="bar.flagUrl"
                    class="country-flag"
                    :src="bar.flagUrl"
                    :alt="`${bar.code} flag`"
                  />
                  <span>{{ bar.code }}</span>
                </strong>
                <span>{{ bar.clicks }}</span>
              </div>
              <div class="bar-track">
                <span class="bar-fill" :style="{ width: `${bar.width}%` }"></span>
              </div>
            </div>
          </div>

          <svg
            class="sparkline"
            :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <defs>
              <linearGradient id="sparklineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#38bdf8" />
                <stop offset="100%" stop-color="#22c55e" />
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
        </div>

        <p v-else-if="analyticsMessage" class="status warning">
          <span class="status-dot"></span>
          {{ analyticsMessage }}
        </p>

        <div class="empty-state" v-else>
          <p class="eyebrow">No data loaded</p>
          <h4 class="card-title">Pick a code from recent links or enter one manually.</h4>
          <p class="muted">
            The dashboard will show total clicks, unique visitors, last click metadata, and top
            countries once a code is loaded.
          </p>
        </div>

        <div v-if="summary" class="stack">
          <div class="grid-2">
            <div class="metric">
              <span class="value">{{ summary?.totalClicks }}</span>
              <span class="label">Total interactions</span>
            </div>
            <div class="metric">
              <span class="value">{{ summary?.redirectClicks }}</span>
              <span class="label">Redirect clicks</span>
            </div>
            <div class="metric">
              <span class="value">{{ summary?.qrScans }}</span>
              <span class="label">QR scans</span>
            </div>
            <div class="metric">
              <span class="value">{{ summary?.uniqueVisitors }}</span>
              <span class="label">Unique visitors</span>
            </div>
          </div>

          <div class="list-item">
            <strong>Last click</strong>
            <p>{{ formatDate(summary?.lastClickedAt ?? null) }}</p>
          </div>
          <div class="list-item">
            <strong>Last referrer</strong>
            <p>{{ summary?.lastReferrer ?? 'No referrer captured yet' }}</p>
          </div>
          <div class="list-item">
            <strong>Browser / device</strong>
            <p>
              {{ summary?.lastBrowserFamily ?? 'Unknown browser' }} -
              {{ summary?.lastDeviceType ?? 'Unknown device' }}
            </p>
          </div>
        </div>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Selected link</p>
          <h3 class="panel-title">Details and quick actions</h3>
        </div>

        <template v-if="link">
          <div class="list-item">
            <strong>{{ link.shortUrl }}</strong>
            <p>Short URL</p>
          </div>
          <div class="list-item">
            <strong>{{ link.originalUrl }}</strong>
            <p>Target URL</p>
          </div>
          <div class="list-item">
            <strong>Owner</strong>
            <p>{{ link.ownerUsername ?? 'Anonymous demo' }}</p>
          </div>
          <div class="list-item-meta">
            <span>Created: {{ formatDate(link.createdAt) }}</span>
            <span>Expires: {{ formatDate(link.expiresAt) }}</span>
          </div>
          <div class="actions">
            <RouterLink :to="{ name: 'link', params: { code: link.code } }">
              <Button label="Open details page" icon="pi pi-external-link" />
            </RouterLink>
            <CopyActionButton
              :value="link.shortUrl"
              label="Copy short URL"
              copied-label="Short URL copied"
            />
            <Button
              type="button"
              label="Open redirect"
              icon="pi pi-arrow-right"
              severity="secondary"
              variant="outlined"
              @click="openExternal(link.shortUrl)"
            />
            <Button
              v-if="canSeePreview"
              type="button"
              label="Open preview JSON"
              icon="pi pi-code"
              severity="secondary"
              variant="outlined"
              @click="openExternal(buildApiBaseUrl(`/urls/${link.code}/preview`, settings))"
            />
          </div>

          <figure class="stack" style="margin: 0">
            <img class="qr-image" :src="link.qrCodeUrl" :alt="`QR code for ${link.code}`" />
            <div class="actions">
              <CopyActionButton
                :value="link.qrCodeUrl"
                label="Copy QR URL"
                copied-label="QR URL copied"
                variant="primary"
              />
              <Button
                type="button"
                label="Open QR"
                icon="pi pi-qrcode"
                severity="secondary"
                variant="outlined"
                @click="openQrModal(link.qrCodeUrl, link.code)"
              />
            </div>
          </figure>
        </template>

        <div v-else class="empty-state">
          <p class="eyebrow">Recent links</p>
          <h4 class="card-title">Use your latest link to jump straight into analytics.</h4>
          <div v-if="hasRecent" class="list">
            <button
              v-for="item in recentLinks"
              :key="item.code"
              class="list-item button-reset"
              type="button"
              @click="loadRecent(item.code)"
            >
              <strong>{{ item.code }}</strong>
              <p>{{ item.shortUrl }}</p>
            </button>
          </div>
          <p v-else class="muted">No saved links yet. Create one from the home page first.</p>
        </div>

        <div v-if="summary?.topCountries.length" class="stack">
          <div>
            <p class="eyebrow">Top countries</p>
            <h4 class="card-title">Where clicks are coming from</h4>
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
    </article>
  </section>

  <teleport to="body">
    <Transition name="session-notice">
      <div v-if="qrModalUrl" class="modal-backdrop" @click.self="closeQrModal">
        <div class="modal-card card">
          <div class="card-inner stack">
            <div class="section-row">
              <div>
                <p class="eyebrow">QR code</p>
                <h3 class="panel-title">{{ qrModalTitle }}</h3>
              </div>
              <button
                class="button button-secondary button-small"
                type="button"
                @click="closeQrModal"
              >
                Close
              </button>
            </div>

            <img
              class="qr-image qr-image--compact modal-qr"
              :src="qrModalUrl"
              :alt="`QR code for ${qrModalTitle}`"
            />
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>
