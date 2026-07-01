import { computed, onMounted, reactive, ref } from 'vue';
import { loadSettings } from '@/shared/services/settings';
import { listAdminUsersPage } from '@/admin/AdminApi';
import type { AdminUserResponse } from '@/shared/types/api';

export function useUsersView() {
  const settings = loadSettings();
  const users = ref<AdminUserResponse[]>([]);
  const loading = ref(false);
  const errorMessage = ref('');
  const pagination = reactive({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });

  const totalUsers = computed(() => pagination.totalElements);
  const adminUsers = computed(() => users.value.filter((user) => user.role === 'ADMIN').length);
  const activeUsers = computed(() => users.value.filter((user) => user.enabled).length);

  function formatDateTime(value: string | null | undefined) {
    if (!value) {
      return 'Never';
    }

    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  function statusLabel(user: AdminUserResponse) {
    if (!user.enabled) {
      return 'Disabled';
    }
    return user.emailVerified ? 'Active' : 'Email pending';
  }

  async function refresh() {
    loading.value = true;
    errorMessage.value = '';

    try {
      const response = await listAdminUsersPage(pagination.page, pagination.size, settings);
      users.value = response.content;
      pagination.page = response.page;
      pagination.size = response.size;
      pagination.totalElements = response.totalElements;
      pagination.totalPages = response.totalPages;
      pagination.first = response.first;
      pagination.last = response.last;
    } catch (error) {
      users.value = [];
      pagination.totalElements = 0;
      pagination.totalPages = 0;
      pagination.first = true;
      pagination.last = true;
      errorMessage.value = error instanceof Error ? error.message : 'Could not load users';
    } finally {
      loading.value = false;
    }
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

  onMounted(() => {
    void refresh();
  });
  return {
    users,
    pagination,
    loading,
    errorMessage,
    totalUsers,
    adminUsers,
    activeUsers,
    formatDateTime,
    statusLabel,
    refresh,
    previousPage,
    nextPage,
  };
}
