<script setup lang="ts">
import './PasswordResetRequestView.css';
import { RouterLink } from 'vue-router';
import AuthNoticeModal from '@/shared/components/AuthNoticeModal.vue';
import { usePasswordResetRequestView } from './PasswordResetRequestView';

const {
  form,
  busy,
  errorMessage,
  successMessage,
  noticeVisible,
  noticeTitle,
  noticeMessage,
  noticeActionLabel,
  title,
  closeNotice,
  handleNoticeAction,
  submit,
} = usePasswordResetRequestView();
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
          <p class="help-text">Enter your email and we will send a password reset link.</p>
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
              {{ busy ? 'Working...' : 'Send link' }}
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
            <RouterLink class="auth-inline-link" to="/auth/signin"> Sign in </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>

  <AuthNoticeModal
    :visible="noticeVisible"
    :title="noticeTitle"
    :message="noticeMessage"
    :action-label="noticeActionLabel"
    @close="closeNotice"
    @action="handleNoticeAction"
  />
</template>
