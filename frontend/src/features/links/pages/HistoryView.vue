<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import { buildApiBaseUrl } from '@/shared/services/http';
import { loadSettings } from '@/shared/services/settings';
import { listLinks } from '@/features/links/repositories/link.repository';
import type { LinkResponse } from '@/shared/types/api';

const settings = loadSettings();
const links = ref<LinkResponse[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const { copy, isCopied } = useCopyAction();

const hasLinks = computed(() => links.value.length > 0);

async function refresh() {
  loading.value = true;
  errorMessage.value = '';

  try {
    links.value = await listLinks(20, settings);
  } catch (error) {
    links.value = [];
    errorMessage.value = error instanceof Error ? error.message : 'Could not load recent links';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  refresh();
});

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Link history"
      title="Recent links"
      description="Browse the latest links saved on the backend, open QR codes, preview JSON, or jump into analytics."
    />

    <PanelCard
      eyebrow="History"
      title="Recent links"
      description="This list comes straight from the Spring Boot browse endpoint, so it matches the data used by the dashboard and detail pages."
    >
      <template #actions>
        <Button
          type="button"
          :label="loading ? 'Refreshing...' : 'Refresh'"
          icon="pi pi-refresh"
          severity="secondary"
          variant="outlined"
          :disabled="loading"
          @click="refresh"
        />
      </template>

      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <div v-if="hasLinks" class="list">
        <div v-for="item in links" :key="item.code" class="list-item">
          <div class="section-row">
            <div>
              <strong>{{ item.code }}</strong>
              <p>{{ item.originalUrl }}</p>
            </div>
            <span class="footnote">{{ formatDate(item.createdAt) }}</span>
          </div>
          <p class="footnote">Owner: {{ item.ownerUsername ?? 'Anonymous demo' }}</p>
          <p class="footnote">{{ item.clickCount }} clicks</p>
          <div class="actions">
            <RouterLink
              class="button button-primary"
              :to="{ name: 'link', params: { code: item.code } }"
            >
              Details
            </RouterLink>
            <RouterLink
              class="button button-secondary"
              :to="{ name: 'dashboard', query: { code: item.code } }"
            >
              Analytics
            </RouterLink>
            <Button
              type="button"
              :label="isCopied(`history-${item.code}`) ? 'Short URL copied' : 'Copy short URL'"
              :icon="isCopied(`history-${item.code}`) ? 'pi pi-check' : 'pi pi-copy'"
              severity="secondary"
              variant="outlined"
              @click="copy(item.shortUrl, `history-${item.code}`)"
            />
            <Button
              type="button"
              label="Open QR"
              icon="pi pi-qrcode"
              severity="secondary"
              variant="outlined"
              @click="openExternal(item.qrCodeUrl)"
            />
            <Button
              type="button"
              label="Preview JSON"
              icon="pi pi-code"
              severity="secondary"
              variant="outlined"
              @click="openExternal(buildApiBaseUrl(`/urls/${item.code}/preview`, settings))"
            />
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p class="eyebrow">No history yet</p>
        <h4 class="card-title">Create a link first and it will appear here.</h4>
        <p class="muted">
          The history list is backed by the URL browse endpoint, so there is no separate client-side
          state to manage.
        </p>
        <RouterLink to="/">
          <Button label="Create link" icon="pi pi-plus" />
        </RouterLink>
      </div>
    </PanelCard>
  </section>
</template>
