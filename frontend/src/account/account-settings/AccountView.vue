<script setup lang="ts">
import './AccountView.css';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import Password from 'primevue/password';
import PageIntro from '@/shared/components/PageIntro.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import { useAccountView } from './AccountView';

const {
  account,
  busy,
  errorMessage,
  linkedAccounts,
  saving,
  passwordErrorMessage,
  successMessage,
  hasGithubIdentity,
  form,
  formatDateTime,
  formatProvider,
  submitPasswordChange,
} = useAccountView();
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="Account"
      title="Account settings"
      description="Review your profile, password, and connected sign-in methods in one place."
    />

    <div class="page-grid two-col account-settings-grid">
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

      <PanelCard
        eyebrow="Security"
        title="Change password"
        description="Choose at least 6 characters with one letter and one number."
        class="account-form-card"
      >
        <p v-if="hasGithubIdentity" class="status">
          <span class="status-dot"></span>
          Password login becomes available after you set a password via reset password.
        </p>

        <form class="form-grid" @submit.prevent="submitPasswordChange">
          <label class="form-field">
            <span class="field-label">Current password</span>
            <Password
              v-model="form.currentPassword"
              autocomplete="current-password"
              placeholder="Current password"
              :feedback="false"
              toggle-mask
              fluid
            />
          </label>
          <label class="form-field">
            <span class="field-label">New password</span>
            <Password
              v-model="form.newPassword"
              autocomplete="new-password"
              placeholder="New password"
              :feedback="false"
              toggle-mask
              fluid
            />
          </label>
          <label class="form-field">
            <span class="field-label">Confirm new password</span>
            <Password
              v-model="form.confirmPassword"
              autocomplete="new-password"
              placeholder="Confirm new password"
              :feedback="false"
              toggle-mask
              fluid
            />
          </label>

          <div class="actions recent-link-actions account-form-actions">
            <Button
              type="submit"
              :label="saving ? 'Updating...' : 'Update'"
              icon="pi pi-shield"
              severity="secondary"
              variant="outlined"
              :disabled="saving"
            />
            <RouterLink to="/auth/forgot-password">
              <Button label="Reset" severity="secondary" variant="outlined" icon="pi pi-refresh" />
            </RouterLink>
          </div>
        </form>

        <p v-if="passwordErrorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ passwordErrorMessage }}
        </p>
        <p v-if="successMessage" class="status" role="status" aria-live="polite">
          <span class="status-dot"></span>
          {{ successMessage }}
        </p>
      </PanelCard>

      <PanelCard
        eyebrow="Identity providers"
        title="Connected sign-ins"
        description="External sign-ins connected to this account."
      >
        <dl v-if="linkedAccounts.length" class="detail-list detail-list--compact">
          <div
            v-for="identity in linkedAccounts"
            :key="`${identity.provider}-${identity.providerLogin}`"
          >
            <dt>{{ formatProvider(identity.provider) }}</dt>
            <dd>{{ identity.providerLogin }}</dd>
          </div>
        </dl>
        <p v-else class="footnote">No social accounts linked yet.</p>
      </PanelCard>
    </div>
  </section>
</template>
