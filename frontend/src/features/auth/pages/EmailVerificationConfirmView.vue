<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { confirmEmailVerification } from '@/features/auth/repositories/auth.repository';

const route = useRoute();
const router = useRouter();
const busy = ref(true);
const errorMessage = ref('');

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

  try {
    const token = getToken();
    if (!token) {
      throw new Error('Verification token is required.');
    }

    await confirmEmailVerification({ token });
    await router.replace({ path: '/auth/signin', query: { verified: '1' } });
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

        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
        <p v-else-if="busy" class="footnote">Verifying your email address...</p>

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
