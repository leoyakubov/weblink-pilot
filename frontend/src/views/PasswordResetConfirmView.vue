<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, RouterLink, useRouter } from 'vue-router';
import { ApiRequestError, confirmPasswordReset } from '@/lib/api';

const route = useRoute();
const router = useRouter();

const form = reactive({
  token: String(route.query.token ?? ''),
  password: '',
});

const busy = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const title = computed(() => 'Set new password');
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

function validateForm(): string {
  if (!form.token.trim()) {
    return 'Reset token is required.';
  }
  if (!form.password.trim()) {
    return 'Password is required.';
  }
  if (!passwordPattern.test(form.password.trim())) {
    return 'Password must use at least 6 characters, including 1 letter and 1 number.';
  }
  return '';
}

function formatError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 401 || error.status === 400) {
      return 'This reset link is invalid or expired.';
    }
    return 'Unable to change password. Please try again.';
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return 'Unable to change password.';
}

async function submit() {
  busy.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const validationError = validateForm();
    if (validationError) {
      errorMessage.value = validationError;
      return;
    }

    await confirmPasswordReset({
      token: form.token.trim(),
      password: form.password.trim(),
    });
    successMessage.value = 'Password updated. You can sign in with the new password now.';
    await router.push('/auth/signin');
  } catch (error) {
    errorMessage.value = formatError(error);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
        </div>

        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Reset token</span>
            <input
              v-model="form.token"
              class="input"
              type="text"
              placeholder="Paste token from email"
            />
          </label>
          <label class="form-field">
            <span class="field-label">New password</span>
            <input
              v-model="form.password"
              class="input"
              type="password"
              placeholder="Enter a new password"
              autocomplete="new-password"
            />
          </label>

          <div class="actions">
            <button class="button button-primary" type="submit" :disabled="busy">
              {{ busy ? 'Working...' : 'Update password' }}
            </button>
          </div>

          <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
            <span class="status-dot"></span>
            {{ errorMessage }}
          </p>
          <p v-else-if="successMessage" class="status" role="status" aria-live="polite">
            <span class="status-dot"></span>
            {{ successMessage }}
          </p>

          <div class="auth-switch">
            <span class="footnote">Need a fresh reset link?</span>
            <RouterLink
              class="button button-secondary button-small auth-link-button"
              to="/auth/forgot-password"
            >
              Request again
            </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
