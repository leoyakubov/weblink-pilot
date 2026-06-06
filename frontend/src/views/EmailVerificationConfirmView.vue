<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { ApiRequestError, confirmEmailVerification } from '@/lib/api';

const route = useRoute();
const busy = ref(true);
const errorMessage = ref('');
const successMessage = ref('');

const title = computed(() => 'Confirm email');

function getToken() {
  const token = route.query.token;
  return typeof token === 'string' ? token : '';
}

function formatError(error: unknown): string {
  if (error instanceof ApiRequestError && error.status === 401) {
    return 'The verification link is invalid or expired.';
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Unable to verify email';
}

async function confirm() {
  busy.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const token = getToken();
    if (!token) {
      throw new Error('Verification token is required.');
    }

    await confirmEmailVerification({ token });
    successMessage.value = 'Your email has been verified. You can sign in now.';
  } catch (error) {
    errorMessage.value = formatError(error);
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  void confirm();
});
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
        </div>

        <p v-if="busy" class="footnote">Verifying your email address...</p>
        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
        <p v-else-if="successMessage" class="status" role="status" aria-live="polite">
          <span class="status-dot"></span>
          {{ successMessage }}
        </p>

        <div class="auth-switch">
          <span class="footnote">Need to sign in?</span>
          <RouterLink
            class="button button-secondary button-small auth-link-button"
            to="/auth/signin"
          >
            Sign in
          </RouterLink>
        </div>
      </div>
    </article>
  </section>
</template>
