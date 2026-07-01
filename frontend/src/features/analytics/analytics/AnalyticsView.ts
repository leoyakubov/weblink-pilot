import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { isAdminUser } from '@/account/AuthSession';
import { loadSettings } from '@/shared/services/settings';
import { getAnalyticsSummary } from '@/features/analytics/AnalyticsApi';
import { getLinkCreatorOptions, listLinks } from '@/features/links/LinksApi';
import type {
  AnalyticsSummaryResponse,
  LinkCreatorOptionResponse,
  LinkResponse,
} from '@/shared/types/api';

export function useAnalyticsView() {
  type AnalyticsRow = {
    link: LinkResponse;
    summary: AnalyticsSummaryResponse | null;
    error: string;
  };

  const route = useRoute();
  const router = useRouter();
  const settings = loadSettings();

  const filters = reactive({
    ownerScope: String(route.query.scope ?? 'all'),
    expirationScope: String(route.query.expiration ?? 'all'),
    creator: String(route.query.creator ?? ''),
  });

  const rows = ref<AnalyticsRow[]>([]);
  const creatorOptions = ref<LinkCreatorOptionResponse[]>([]);
  const loading = ref(false);
  const errorMessage = ref('');
  const canFilterByCreator = computed(() => isAdminUser());

  const visibleRows = computed(() => rows.value);

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

  async function loadAnalyticsOverview() {
    loading.value = true;
    errorMessage.value = '';

    try {
      const creator = backendCreatorFilter();
      const ownerRole = backendOwnerRoleFilter();
      const expiration = backendExpirationFilter();
      const links = await listLinks(20, settings, creator, ownerRole, expiration);
      const analyticsRows = await Promise.all(
        links.map(async (link) => {
          try {
            return {
              link,
              summary: await getAnalyticsSummary(link.code, settings),
              error: '',
            };
          } catch (error) {
            const forbidden =
              error instanceof ApiRequestError && (error.status === 401 || error.status === 403);
            return {
              link,
              summary: null,
              error: forbidden
                ? 'Analytics are not available for this link.'
                : error instanceof Error
                  ? error.message
                  : 'Could not load analytics.',
            };
          }
        }),
      );
      rows.value = analyticsRows;
      router.replace({
        query: {
          ...(canFilterByCreator.value && filters.ownerScope !== 'all'
            ? { scope: filters.ownerScope }
            : {}),
          ...(canFilterByCreator.value && filters.creator.trim()
            ? { creator: filters.creator.trim() }
            : {}),
          ...(filters.expirationScope !== 'all' ? { expiration: filters.expirationScope } : {}),
        },
      });
    } catch (error) {
      rows.value = [];
      errorMessage.value = error instanceof Error ? error.message : 'Could not load analytics.';
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

  function scopeLabel() {
    if (!canFilterByCreator.value) {
      return 'available links';
    }

    if (filters.creator.trim()) {
      return `creator "${filters.creator.trim()}"`;
    }

    const labels: Record<string, string> = {
      all: 'all links',
      admins: 'admin-owned links',
      users: 'user-owned links',
      anonymous: 'anonymous links',
    };
    return labels[filters.ownerScope] ?? 'all links';
  }

  function ownerLabel(link: LinkResponse) {
    return link.ownerUsername ?? 'anonymous';
  }

  function ownerRoleLabel(link: LinkResponse) {
    return link.ownerRole ?? (link.ownerUsername ? 'USER' : 'ANONYMOUS');
  }

  function formatDate(value: string | null) {
    if (!value) {
      return 'No interactions yet';
    }

    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  onMounted(async () => {
    await loadCreatorOptions();
    void loadAnalyticsOverview();
  });
  return {
    filters,
    rows,
    creatorOptions,
    loading,
    errorMessage,
    canFilterByCreator,
    visibleRows,
    loadAnalyticsOverview,
    scopeLabel,
    ownerLabel,
    ownerRoleLabel,
    formatDate,
  };
}
