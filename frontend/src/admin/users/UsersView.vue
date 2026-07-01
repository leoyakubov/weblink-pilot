<script setup lang="ts">
import './UsersView.css';
import PageIntro from '@/shared/components/PageIntro.vue';
import PaginationControls from '@/shared/components/PaginationControls.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import RefreshButton from '@/shared/components/RefreshButton.vue';
import { useUsersView } from './UsersView';

const {
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
} = useUsersView();
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
          <dt>Admins on page</dt>
          <dd>{{ adminUsers }}</dd>
        </div>
        <div>
          <dt>Active on page</dt>
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

      <PaginationControls
        v-if="users.length || pagination.totalElements"
        :page="pagination.page"
        :size="pagination.size"
        :total-elements="pagination.totalElements"
        :total-pages="pagination.totalPages"
        :first="pagination.first"
        :last="pagination.last"
        :loading="loading"
        @previous="previousPage"
        @next="nextPage"
      />
    </PanelCard>
  </section>
</template>
