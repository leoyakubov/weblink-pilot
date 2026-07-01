import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { createCopyAction } from '@/shared/composables/CopyAction';
import { isAdminUser } from '@/account/AuthSession';
import { buildApiBaseUrl } from '@/shared/services/http';
import { loadSettings } from '@/shared/services/settings';
import { getAiLinkMetadata, getLink, regenerateAiLinkMetadata } from '@/features/links/LinksApi';
import type { AiLinkMetadataResponse, LinkResponse } from '@/shared/types/api';

export function useLinkView() {
  const route = useRoute();
  const settings = loadSettings();

  const link = ref<LinkResponse | null>(null);
  const aiMetadata = ref<AiLinkMetadataResponse | null>(null);
  const loading = ref(false);
  const regeneratingAi = ref(false);
  const errorMessage = ref('');
  const qrModalUrl = ref('');
  const qrModalTitle = ref('');
  const { copy, isCopied } = createCopyAction();

  const code = computed(() => String(route.params.code ?? ''));
  const canSeePreview = computed(() => isAdminUser());

  async function load(codeValue: string) {
    if (!codeValue) {
      return;
    }

    loading.value = true;
    errorMessage.value = '';

    try {
      link.value = await getLink(codeValue, settings);
      try {
        aiMetadata.value = await getAiLinkMetadata(codeValue, settings);
      } catch {
        aiMetadata.value = null;
      }
    } catch (error) {
      link.value = null;
      aiMetadata.value = null;
      errorMessage.value = error instanceof Error ? error.message : 'Could not load link details';
    } finally {
      loading.value = false;
    }
  }

  const aiMetadataStatus = computed(() => aiMetadata.value?.status ?? 'PENDING');

  const aiMetadataTags = computed(() => aiMetadata.value?.tags ?? []);

  const regenerateAiLabel = computed(() => {
    if (regeneratingAi.value) {
      return 'Regenerating';
    }
    return aiMetadataStatus.value === 'FAILED' ? 'Retry' : 'Regenerate';
  });

  onMounted(() => load(code.value));
  watch(code, (value) => load(value));

  function openExternal(url: string) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  function openQrModal(url: string, title: string) {
    qrModalUrl.value = url;
    qrModalTitle.value = title;
  }

  async function regenerateAiMetadata() {
    if (!link.value || regeneratingAi.value) {
      return;
    }

    regeneratingAi.value = true;
    try {
      aiMetadata.value = await regenerateAiLinkMetadata(link.value.code, settings);
    } finally {
      regeneratingAi.value = false;
    }
  }

  async function shareUrl(url: string, label: string, title: string) {
    try {
      const shareApi = window.navigator as {
        share?: (data: { title: string; text: string; url: string }) => Promise<void>;
      };

      if (shareApi.share) {
        await shareApi.share({
          title,
          text: title,
          url,
        });
        return;
      }

      await copy(url, label);
    } catch {
      // Native share can be cancelled by the user; keep the page state unchanged.
    }
  }

  const qrImage = computed(() => link.value?.qrCodeUrl ?? '');

  function closeQrModal() {
    qrModalUrl.value = '';
    qrModalTitle.value = '';
  }

  function formatDate(value: string | null, fallback = 'Never') {
    if (!value) {
      return fallback;
    }

    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }
  return {
    link,
    aiMetadata,
    loading,
    regeneratingAi,
    errorMessage,
    buildApiBaseUrl,
    qrModalUrl,
    qrModalTitle,
    settings,
    copy,
    isCopied,
    code,
    canSeePreview,
    aiMetadataStatus,
    aiMetadataTags,
    regenerateAiLabel,
    openExternal,
    openQrModal,
    regenerateAiMetadata,
    shareUrl,
    qrImage,
    closeQrModal,
    formatDate,
  };
}
