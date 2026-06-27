<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import { isAdminUser } from '@/features/auth/services/auth.service';
import LinkFilters from '@/features/links/components/LinkFilters.vue';
import LinkList from '@/features/links/components/LinkList.vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import QrCodeModal from '@/shared/components/common/QrCodeModal.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import { loadSettings } from '@/shared/services/settings';
import { getLinkCreatorOptions, listLinks } from '@/features/links/repositories/link.repository';
import type { LinkCreatorOptionResponse, LinkResponse } from '@/shared/types/api';

const settings = loadSettings();
const filters = reactive({
  ownerScope: 'all',
  expirationScope: 'all',
  creator: '',
});
const links = ref<LinkResponse[]>([]);
const creatorOptions = ref<LinkCreatorOptionResponse[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const qrModalUrl = ref('');
const qrModalTitle = ref('');

const hasLinks = computed(() => links.value.length > 0);
const canFilterByCreator = computed(() => isAdminUser());

function backendCreatorFilter() {
  if (!canFilterByCreator.value) {
    return '';
  }

  const exactCreator = filters.creator.trim();
  if (exactCreator) {
    return exactCreator;
  }

  return '';
}

function backendOwnerRoleFilter() {
  if (!canFilterByCreator.value || filters.creator.trim()) {
    return '';
  }

  const roles: Record<string, string> = {
    admins: 'ADMIN',
    users: 'USER',
    anonymous: 'ANONYMOUS',
  };
  return roles[filters.ownerScope] ?? '';
}

function backendExpirationFilter() {
  const expirations: Record<string, string> = {
    active: 'ACTIVE',
    expired: 'EXPIRED',
    never: 'NEVER',
  };
  return expirations[filters.expirationScope] ?? '';
}

function scopeLabel() {
  if (!canFilterByCreator.value) {
    return 'links available to your session';
  }

  if (filters.creator.trim()) {
    return `links created by "${filters.creator.trim()}"`;
  }

  const labels: Record<string, string> = {
    all: 'all active links',
    admins: 'admin-owned links',
    users: 'user-owned links',
    anonymous: 'anonymous links',
  };
  return labels[filters.ownerScope] ?? 'all active links';
}

async function refresh() {
  loading.value = true;
  errorMessage.value = '';

  try {
    links.value = await listLinks(
      20,
      settings,
      backendCreatorFilter(),
      backendOwnerRoleFilter(),
      backendExpirationFilter(),
    );
  } catch (error) {
    links.value = [];
    errorMessage.value = error instanceof Error ? error.message : 'Could not load recent links';
  } finally {
    loading.value = false;
  }
}

async function loadCreatorOptions() {
  if (!canFilterByCreator.value) {
    creatorOptions.value = [];
    return;
  }

  creatorOptions.value = await getLinkCreatorOptions(settings);
}

onMounted(async () => {
  await loadCreatorOptions();
  void refresh();
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
      eyebrow="Links"
      title="Saved links"
      description="Manage the links available to you, copy short URLs, open QR codes, and review activity."
    />

    <PanelCard
      eyebrow="Library"
      title="Latest links"
      :description="`Showing ${scopeLabel()} with quick actions for sharing and analytics.`"
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="refresh" />
      </template>

      <LinkFilters
        v-model:owner-scope="filters.ownerScope"
        v-model:expiration-scope="filters.expirationScope"
        v-model:creator="filters.creator"
        :creator-options="creatorOptions"
        :show-admin-filters="canFilterByCreator"
        :loading="loading"
        @apply="refresh"
      />

      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <LinkList
        v-if="hasLinks"
        :links="links"
        copy-key-prefix="links"
        @open-qr="(item) => openQrModal(item.qrCodeUrl, item.code)"
      />

      <div v-else class="empty-state">
        <p class="eyebrow">No links yet</p>
        <h4 class="card-title">Create a link first and it will appear here.</h4>
        <p class="muted">Saved links appear here as soon as they are created.</p>
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
