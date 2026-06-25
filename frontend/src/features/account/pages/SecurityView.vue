<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import Password from 'primevue/password';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { changePassword } from '@/features/account/repositories/account.repository';
import { useAccountProfile } from '@/features/account/composables/useAccountProfile';
import { ApiRequestError } from '@/shared/services/http';

const { linkedAccounts, loadAccount } = useAccountProfile();
const saving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

function resetMessages() {
  errorMessage.value = '';
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
  resetMessages();
  try {
    const validationError = validatePassword();
    if (validationError) {
      errorMessage.value = validationError;
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
    errorMessage.value = formatChangeError(error);
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
      title="Security"
      description="Update your password and review external identity providers connected to this account."
    />

    <div class="page-grid two-col account-settings-grid">
      <PanelCard
        eyebrow="Security"
        title="Change password"
        description="Use a password with at least 6 characters, including one letter and one number."
        class="account-form-card"
      >
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

          <div class="actions">
            <Button
              type="submit"
              :label="saving ? 'Updating...' : 'Update password'"
              icon="pi pi-shield"
              :disabled="saving"
            />
            <RouterLink to="/auth/forgot-password">
              <Button label="Reset password instead" severity="secondary" variant="outlined" />
            </RouterLink>
          </div>
        </form>

        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
        <p v-if="successMessage" class="status" role="status" aria-live="polite">
          <span class="status-dot"></span>
          {{ successMessage }}
        </p>
      </PanelCard>

      <PanelCard
        eyebrow="Identity providers"
        title="Connected sign-ins"
        description="If you use GitHub sign-in, a local password is still useful for recovery."
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
