<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import LinkList from '@/features/links/components/LinkList.vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import QrCodeModal from '@/shared/components/common/QrCodeModal.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import { loadSettings } from '@/shared/services/settings';
import { listLinks } from '@/features/links/repositories/link.repository';
import type { LinkResponse } from '@/shared/types/api';

const settings = loadSettings();
const links = ref<LinkResponse[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const qrModalUrl = ref('');
const qrModalTitle = ref('');

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

function openQrModal(url: string, title: string) {
  qrModalUrl.value = url;
  qrModalTitle.value = title;
}

function closeQrModal() {
  qrModalUrl.value = '';
  qrModalTitle.value = '';
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
        <RefreshButton :loading="loading" @refresh="refresh" />
      </template>

      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <LinkList
        v-if="hasLinks"
        :links="links"
        copy-key-prefix="history"
        @open-qr="(item) => openQrModal(item.qrCodeUrl, item.code)"
      />

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

    <QrCodeModal
      :visible="Boolean(qrModalUrl)"
      :title="qrModalTitle"
      :url="qrModalUrl"
      @close="closeQrModal"
    />
  </section>
</template>
