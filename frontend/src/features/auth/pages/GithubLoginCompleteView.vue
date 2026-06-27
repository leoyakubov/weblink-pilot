<script setup lang="ts">
/* global URLSearchParams */
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { completeGithubLogin } from '@/features/auth/repositories/auth.repository';
import { applyAuthResponse } from '@/features/auth/services/auth.service';
import type { AuthResponse } from '@/shared/types/api';

const router = useRouter();
const busy = ref(true);
const errorMessage = ref('');

function readTicket() {
  const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : '';
  const hashParams = new URLSearchParams(hash);
  return (
    hashParams.get('ticket') ?? new URLSearchParams(window.location.search).get('ticket') ?? ''
  );
}

function postLoginMessage(response: AuthResponse) {
  if (window.opener && !window.opener.closed) {
    window.opener.postMessage(
      {
        type: 'weblinkpilot:github-login',
        response,
      },
      window.location.origin,
    );
    window.close();
    return true;
  }

  return false;
}

async function finishLogin() {
  try {
    const ticket = readTicket();
    if (!ticket) {
      throw new Error('Missing GitHub login ticket.');
    }

    const response = await completeGithubLogin({ ticket });
    if (!postLoginMessage(response)) {
      applyAuthResponse(response, `Signed in as ${response.username}`);
      await router.replace('/');
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'GitHub login failed.';
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  void finishLogin();
});
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">GitHub sign-in</h3>
          <p class="help-text">Finishing the GitHub handoff and returning you to WebLinkPilot.</p>
        </div>

        <p v-if="busy && !errorMessage" class="footnote">Completing GitHub sign-in...</p>
        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
      </div>
    </article>
  </section>
</template>
