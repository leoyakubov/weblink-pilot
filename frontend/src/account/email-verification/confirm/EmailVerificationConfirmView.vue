<script setup lang="ts">
import './EmailVerificationConfirmView.css';
import { RouterLink } from 'vue-router';
import { useEmailVerificationConfirmView } from './EmailVerificationConfirmView';

const { busy, errorMessage, title } = useEmailVerificationConfirmView();
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
          <p class="help-text">We are checking the verification link from your email.</p>
        </div>

        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
        <p v-else-if="busy" class="footnote">Verifying your email...</p>

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
