<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import CopyActionButton from '@/shared/components/CopyActionButton.vue';
import { buildApiBaseUrl, listLinks } from '@/lib/api';
import { loadSettings } from '@/lib/settings';
import type { LinkResponse } from '@/types';

const settings = loadSettings();
const links = ref<LinkResponse[]>([]);
const loading = ref(false);
const errorMessage = ref('');

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
    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">Link history</p>
            <h3 class="panel-title">Recent links from the backend.</h3>
          </div>
          <button
            class="button button-secondary"
            type="button"
            :disabled="loading"
            @click="refresh"
          >
            {{ loading ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>

        <p class="help-text">
          This list now comes from the Spring Boot browse endpoint, so it reflects the same data
          your dashboard and details pages use.
        </p>

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
              <CopyActionButton
                :value="item.shortUrl"
                label="Copy short URL"
                copied-label="Short URL copied"
              />
              <button
                class="button button-secondary"
                type="button"
                @click="openExternal(item.qrCodeUrl)"
              >
                Open QR
              </button>
              <button
                class="button button-secondary"
                type="button"
                @click="openExternal(buildApiBaseUrl(`/urls/${item.code}/preview`, settings))"
              >
                Preview JSON
              </button>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p class="eyebrow">No history yet</p>
          <h4 class="card-title">Create a link first and it will appear here.</h4>
          <p class="muted">
            The history list is backed by the URL browse endpoint, so there is no separate
            client-side state to manage.
          </p>
          <RouterLink class="button button-primary" to="/"> Create link </RouterLink>
        </div>
      </div>
    </article>
  </section>
</template>
