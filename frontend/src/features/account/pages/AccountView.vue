<script setup lang="ts">
import { onMounted } from 'vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { useAccountProfile } from '@/features/account/composables/useAccountProfile';

const { account, busy, errorMessage, linkedAccounts, loadAccount } = useAccountProfile();

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
      description="Review your signed-in identity, role, email state, and linked providers."
    />

    <div class="page-grid two-col account-settings-grid">
      <PanelCard
        eyebrow="Profile"
        title="Account details"
        description="Compact account metadata for the current signed-in session."
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

      <PanelCard
        eyebrow="Linked accounts"
        title="Identity providers"
        description="External sign-in methods connected to this account."
      >
        <div v-if="linkedAccounts.length" class="stack">
          <div
            v-for="identity in linkedAccounts"
            :key="`${identity.provider}-${identity.providerLogin}`"
            class="linked-account"
          >
            <strong>{{ identity.provider }}</strong>
            <span class="footnote">{{ identity.providerLogin }}</span>
          </div>
        </div>
        <p v-else class="footnote">No social accounts linked yet.</p>
      </PanelCard>
    </div>
  </section>
</template>
