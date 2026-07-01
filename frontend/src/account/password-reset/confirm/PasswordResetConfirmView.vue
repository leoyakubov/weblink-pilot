<script setup lang="ts">
import './PasswordResetConfirmView.css';
import { RouterLink } from 'vue-router';
import { usePasswordResetConfirmView } from './PasswordResetConfirmView';

const { form, busy, errorMessage, successMessage, title, submit } = usePasswordResetConfirmView();
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
          <p class="help-text">Use the reset token from your email to choose a new password.</p>
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
              {{ busy ? 'Working...' : 'Update' }}
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
            <RouterLink class="auth-inline-link" to="/auth/forgot-password">
              Request again
            </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
