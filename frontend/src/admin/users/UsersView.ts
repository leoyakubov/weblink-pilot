import { computed, onMounted, ref } from 'vue';
import { loadSettings } from '@/shared/services/settings';
import { listAdminUsers } from '@/admin/AdminApi';
import type { AdminUserResponse } from '@/shared/types/api';

export function useUsersView() {
  const settings = loadSettings();
  const users = ref<AdminUserResponse[]>([]);
  const loading = ref(false);
  const errorMessage = ref('');

  const totalUsers = computed(() => users.value.length);
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
      users.value = await listAdminUsers(settings);
    } catch (error) {
      users.value = [];
      errorMessage.value = error instanceof Error ? error.message : 'Could not load users';
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    void refresh();
  });
  return {
    users,
    loading,
    errorMessage,
    totalUsers,
    adminUsers,
    activeUsers,
    formatDateTime,
    statusLabel,
    refresh,
  };
}
