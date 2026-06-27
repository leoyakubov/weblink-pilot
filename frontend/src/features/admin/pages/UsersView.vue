<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import { loadSettings } from '@/shared/services/settings';
import { listAdminUsers } from '@/features/admin/repositories/admin.repository';
import type { AdminUserResponse } from '@/shared/types/api';

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
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Admin"
      title="Users"
      description="Review registered accounts, roles, access status, and recent sign-in activity."
    >
      <template #actions>
        <RefreshButton :loading="loading" @refresh="refresh" />
      </template>
    </PageIntro>

    <PanelCard
      class="half-panel"
      eyebrow="Stats"
      title="User stats"
      description="A quick account summary for this WeblinkPilot environment."
    >
      <p v-if="errorMessage" class="status error">
        <span class="status-dot"></span>
        {{ errorMessage }}
      </p>

      <dl class="detail-list detail-list--link-detail detail-list--analytics admin-users-summary">
        <div>
          <dt>Total users</dt>
          <dd>{{ totalUsers }}</dd>
        </div>
        <div>
          <dt>Admins</dt>
          <dd>{{ adminUsers }}</dd>
        </div>
        <div>
          <dt>Active</dt>
          <dd>{{ activeUsers }}</dd>
        </div>
      </dl>
    </PanelCard>

    <PanelCard
      eyebrow="User management"
      title="User directory"
      description="Admin-only account directory for checking identities, roles, and access state."
    >
      <div v-if="users.length" class="admin-users-table" role="table">
        <div class="admin-users-table__header" role="row">
          <span>User</span>
          <span>Role</span>
          <span>Status</span>
          <span>Email</span>
          <span>Created</span>
          <span>Last login</span>
        </div>

        <div v-for="user in users" :key="user.username" class="admin-users-table__row" role="row">
          <strong>{{ user.username }}</strong>
          <span>{{ user.role.toLowerCase() }}</span>
          <span>
            {{ statusLabel(user) }}
          </span>
          <span>{{ user.email ?? 'No email' }}</span>
          <span>{{ formatDateTime(user.createdAt) }}</span>
          <span>{{ formatDateTime(user.lastLoginAt) }}</span>
        </div>
      </div>

      <div v-else-if="!loading" class="empty-state">
        <p class="eyebrow">No users</p>
        <h4 class="card-title">No accounts were returned by the backend.</h4>
        <p class="muted">Refresh after the backend starts or after account seed data is loaded.</p>
      </div>
    </PanelCard>
  </section>
</template>
