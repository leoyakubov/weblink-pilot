<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import Password from 'primevue/password';
import { ApiRequestError } from '@/shared/services/http';
import {
  changePassword,
  getAccountProfile,
} from '@/features/account/repositories/account.repository';
import type { AccountProfileResponse } from '@/shared/types/api';

const busy = ref(true);
const saving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const account = ref<AccountProfileResponse | null>(null);
const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

const linkedAccounts = computed(() => account.value?.socialIdentities ?? []);

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

async function loadAccount() {
  busy.value = true;
  resetMessages();
  try {
    account.value = await getAccountProfile();
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 401) {
      errorMessage.value = 'Please sign in again to view your account settings.';
    } else {
      errorMessage.value =
        error instanceof Error ? error.message : 'Unable to load account settings.';
    }
  } finally {
    busy.value = false;
  }
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
  <section class="stack">
    <article class="card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">Your profile</h3>
        </div>

        <p v-if="busy" class="footnote">Loading account details...</p>
        <template v-else-if="account">
          <div class="meta-grid">
            <div class="meta-item">
              <strong>Username</strong>
              <p>{{ account.username }}</p>
            </div>
            <div class="meta-item">
              <strong>Role</strong>
              <p>{{ account.role }}</p>
            </div>
            <div class="meta-item">
              <strong>Email</strong>
              <p>{{ account.email ?? 'Not set' }}</p>
            </div>
            <div class="meta-item">
              <strong>Email verified</strong>
              <p>{{ account.emailVerified ? 'Yes' : 'No' }}</p>
            </div>
          </div>

          <div class="meta-grid">
            <div class="meta-item">
              <strong>Created at</strong>
              <p>{{ account.createdAt }}</p>
            </div>
            <div class="meta-item">
              <strong>Last login</strong>
              <p>{{ account.lastLoginAt ?? 'Never' }}</p>
            </div>
          </div>
        </template>

        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Linked accounts</p>
          <h3 class="panel-title">Identity providers</h3>
        </div>

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

        <p class="footnote">
          If you sign in with GitHub only, you can still set a local password here for recovery.
        </p>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Security</p>
          <h3 class="panel-title">Change password</h3>
        </div>

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

        <p v-if="successMessage" class="status" role="status" aria-live="polite">
          <span class="status-dot"></span>
          {{ successMessage }}
        </p>
      </div>
    </article>
  </section>
</template>
