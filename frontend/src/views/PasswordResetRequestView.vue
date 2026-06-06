<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ApiRequestError, requestPasswordReset } from '@/lib/api';

const form = reactive({
  email: '',
});

const busy = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const title = computed(() => 'Reset password');

function validateForm(): string {
  if (!form.email.trim()) {
    return 'Email is required.';
  }
  return '';
}

function formatError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.status === 400) {
      return 'Enter a valid email address.';
    }
    return 'Unable to request a password reset. Please try again.';
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return 'Unable to request a password reset.';
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

    await requestPasswordReset({ email: form.email.trim() });
    successMessage.value =
      'If the email exists, a reset link was sent. Check your inbox or the local Mailpit UI.';
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
            <span class="field-label">Email</span>
            <input
              v-model="form.email"
              class="input"
              type="email"
              placeholder="you@example.com"
              autocomplete="email"
            />
          </label>

          <div class="actions">
            <button class="button button-primary" type="submit" :disabled="busy">
              {{ busy ? 'Working...' : 'Send reset link' }}
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
            <span class="footnote">Remembered it?</span>
            <RouterLink
              class="button button-secondary button-small auth-link-button"
              to="/auth/signin"
            >
              Sign in
            </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
