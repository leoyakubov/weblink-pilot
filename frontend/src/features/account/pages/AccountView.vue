<script setup lang="ts">
import { onMounted } from 'vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { useAccountProfile } from '@/features/account/composables/useAccountProfile';

const { account, busy, errorMessage, loadAccount } = useAccountProfile();

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Never';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(() => {
  void loadAccount();
});
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Account"
      title="Profile"
      description="Review the identity, role, email status, and recent activity for your signed-in account."
    />

    <div class="page-grid account-settings-grid account-settings-grid--single">
      <PanelCard
        eyebrow="Profile"
        title="Account details"
        description="The key details currently connected to your session."
        class="account-form-card"
      >
        <p v-if="busy" class="footnote">Loading account details...</p>
        <dl v-else-if="account" class="detail-list detail-list--compact">
          <div>
            <dt>Username</dt>
            <dd>{{ account.username }}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{{ account.role }}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{{ account.email ?? 'Not set' }}</dd>
          </div>
          <div>
            <dt>Email verified</dt>
            <dd>{{ account.emailVerified ? 'Yes' : 'No' }}</dd>
          </div>
          <div>
            <dt>Created at</dt>
            <dd>{{ formatDateTime(account.createdAt) }}</dd>
          </div>
          <div>
            <dt>Last login</dt>
            <dd>{{ formatDateTime(account.lastLoginAt) }}</dd>
          </div>
        </dl>

        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
      </PanelCard>
    </div>
  </section>
</template>
