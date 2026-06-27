<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import Password from 'primevue/password';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { changePassword } from '@/features/account/repositories/account.repository';
import { useAccountProfile } from '@/features/account/composables/useAccountProfile';
import { ApiRequestError } from '@/shared/services/http';

const { account, busy, errorMessage, linkedAccounts, loadAccount } = useAccountProfile();
const saving = ref(false);
const passwordErrorMessage = ref('');
const successMessage = ref('');
const hasGithubIdentity = computed(() =>
  linkedAccounts.value.some((identity) => identity.provider.toUpperCase() === 'GITHUB'),
);
const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Never';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatProvider(provider: string) {
  if (provider.toUpperCase() === 'GITHUB') {
    return 'GitHub';
  }

  return provider.toLowerCase().replace(/(^|[-_\s])\w/g, (match) => match.toUpperCase());
}

function resetPasswordMessages() {
  passwordErrorMessage.value = '';
  successMessage.value = '';
}

function validatePassword(): string {
  if (!form.currentPassword.trim()) {
    return 'Enter your current password.';
  }
  if (!form.newPassword.trim()) {
    return 'Enter a new password.';
  }
  if (!passwordPattern.test(form.newPassword)) {
    return 'Password must use at least 6 characters, including 1 letter and 1 number.';
  }
  if (form.newPassword !== form.confirmPassword) {
    return 'Passwords do not match.';
  }
  return '';
}

function formatChangeError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 401) {
      return 'Current password is incorrect.';
    }
    if (error.status === 400) {
      return error.message || 'Unable to update password.';
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return 'Unable to update password.';
}

async function submitPasswordChange() {
  saving.value = true;
  resetPasswordMessages();
  try {
    const validationError = validatePassword();
    if (validationError) {
      passwordErrorMessage.value = validationError;
      return;
    }

    await changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    });

    form.currentPassword = '';
    form.newPassword = '';
    form.confirmPassword = '';
    successMessage.value = 'Password updated. Other refresh sessions were revoked.';
  } catch (error) {
    passwordErrorMessage.value = formatChangeError(error);
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  void loadAccount();
});
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
