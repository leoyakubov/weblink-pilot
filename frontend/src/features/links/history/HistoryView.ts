import { computed, onMounted, reactive, ref } from 'vue';
import { isAdminUser } from '@/account/AuthSession';
import { loadSettings } from '@/shared/services/settings';
import { getLinkCreatorOptions, listLinksPage } from '@/features/links/LinksApi';
import type { LinkCreatorOptionResponse, LinkResponse } from '@/shared/types/api';

export function useHistoryView() {
  const pageSize = 10;
  const settings = loadSettings();
  const filters = reactive({
    ownerScope: 'all',
    expirationScope: 'all',
    creator: '',
  });
  const pagination = reactive({
    page: 0,
    size: pageSize,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
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
      const response = await listLinksPage(
        pagination.page,
        pagination.size,
        settings,
        backendCreatorFilter(),
        backendOwnerRoleFilter(),
        backendExpirationFilter(),
      );
      links.value = response.content;
      pagination.page = response.page;
      pagination.size = response.size;
      pagination.totalElements = response.totalElements;
      pagination.totalPages = response.totalPages;
      pagination.first = response.first;
      pagination.last = response.last;
    } catch (error) {
      links.value = [];
      pagination.totalElements = 0;
      pagination.totalPages = 0;
      pagination.first = true;
      pagination.last = true;
      errorMessage.value = error instanceof Error ? error.message : 'Could not load recent links';
    } finally {
      loading.value = false;
    }
  }

  function applyFilters() {
    pagination.page = 0;
    void refresh();
  }

  function previousPage() {
    if (pagination.first || loading.value) {
      return;
    }
    pagination.page -= 1;
    void refresh();
  }

  function nextPage() {
    if (pagination.last || loading.value) {
      return;
    }
    pagination.page += 1;
    void refresh();
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
  return {
    filters,
    pagination,
    links,
    creatorOptions,
    loading,
    errorMessage,
    qrModalUrl,
    qrModalTitle,
    hasLinks,
    canFilterByCreator,
    scopeLabel,
    refresh,
    applyFilters,
    previousPage,
    nextPage,
    openQrModal,
    closeQrModal,
  };
}
