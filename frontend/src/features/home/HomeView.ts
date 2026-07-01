import { computed, onMounted, reactive, ref, watch } from 'vue';
import { createCopyAction } from '@/shared/composables/CopyAction';
import { authState } from '@/account/AuthSession';
import { buildApiBaseUrl } from '@/shared/services/http';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import { createLink, listLinks } from '@/features/links/LinksApi';
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/shared/types/api';

const CREATE_COOLDOWN_MS = 1500;

export function useHomeView() {
  const settings = reactive<ApiSettings>(loadSettings());
  const form = reactive<CreateLinkRequest>({
    originalUrl: 'https://github.com/leoyakubov/weblink-pilot',
    customAlias: '',
    expiresAt: '',
  });

  const createdLink = ref<LinkResponse | null>(null);
  const recentLinks = ref<LinkResponse[]>([]);
  const loadingRecent = ref(false);
  const recentError = ref('');
  const errorMessage = ref('');
  const successMessage = ref('');
  const submitting = ref(false);
  const createCooldownUntil = ref(0);
  const qrModalUrl = ref('');
  const qrModalTitle = ref('');
  const createdModalOpen = ref(false);
  const createdModalMode = ref<'created' | 'existing'>('created');

  const userStatus = computed(() =>
    authState.currentUser
      ? `Signed in as ${authState.currentUser.username} (${authState.currentUser.role})`
      : 'Guest mode (ready for anonymous links)',
  );

  const canSeePreview = computed(() => authState.currentUser?.role === 'ADMIN');

  const linkPreviewUrl = computed(() =>
    createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
  );

  const { copy, isCopied } = createCopyAction();

  function normalizeComparableUrl(value: string) {
    try {
      return new URL(value.trim()).href;
    } catch {
      return value.trim();
    }
  }

  function findExistingLink(originalUrl: string) {
    const currentOwner = authState.currentUser?.username ?? null;
    const normalizedOriginalUrl = normalizeComparableUrl(originalUrl);

    return recentLinks.value.find(
      (link) =>
        normalizeComparableUrl(link.originalUrl) === normalizedOriginalUrl &&
        (link.ownerUsername ?? null) === currentOwner,
    );
  }

  function syncSettings() {
    saveSettings(settings);
  }

  async function refreshRecent() {
    loadingRecent.value = true;
    recentError.value = '';

    try {
      recentLinks.value = await listLinks(5, settings);
    } catch (error) {
      recentLinks.value = [];
      recentError.value = error instanceof Error ? error.message : 'Could not load recent links';
    } finally {
      loadingRecent.value = false;
    }
  }

  async function submit() {
    errorMessage.value = '';
    successMessage.value = '';

    try {
      syncSettings();

      const originalUrl = form.originalUrl.trim();
      new URL(originalUrl);

      const now = Date.now();
      if (now < createCooldownUntil.value) {
        errorMessage.value = 'Please wait a moment before shortening another link.';
        return;
      }

      const existingLink = findExistingLink(originalUrl);
      if (existingLink) {
        createdLink.value = existingLink;
        createdModalMode.value = 'existing';
        createdModalOpen.value = true;
        return;
      }

      const payload: CreateLinkRequest = {
        originalUrl,
        customAlias: form.customAlias?.trim() || undefined,
        expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
      };

      submitting.value = true;
      createCooldownUntil.value = Date.now() + CREATE_COOLDOWN_MS;
      createdLink.value = await createLink(payload, settings);
      createdModalMode.value = 'created';
      successMessage.value = `Created ${createdLink.value.code} successfully`;
      createdModalOpen.value = true;
      await refreshRecent();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Something went wrong';
    } finally {
      submitting.value = false;
    }
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

  function closeCreatedModal() {
    createdModalOpen.value = false;
  }

  onMounted(() => {
    refreshRecent();
  });

  watch(
    () => authState.currentUser?.username,
    () => {
      refreshRecent();
    },
  );

  return {
    canSeePreview,
    closeCreatedModal,
    closeQrModal,
    copy,
    createdLink,
    createdModalMode,
    createdModalOpen,
    errorMessage,
    form,
    isCopied,
    linkPreviewUrl,
    loadingRecent,
    openExternal,
    openQrModal,
    qrModalTitle,
    qrModalUrl,
    recentError,
    recentLinks,
    refreshRecent,
    submit,
    submitting,
    userStatus,
  };
}
