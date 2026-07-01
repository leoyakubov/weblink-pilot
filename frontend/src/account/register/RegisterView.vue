<script setup lang="ts">
import './RegisterView.css';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import AuthNoticeModal from '@/shared/components/AuthNoticeModal.vue';
import { useRegisterView } from './RegisterView';

const {
  form,
  busy,
  errorMessage,
  noticeVisible,
  noticeTitle,
  noticeMessage,
  noticeActionLabel,
  title,
  submitLabel,
  passwordAutocomplete,
  closeNotice,
  handleNoticeAction,
  openGithubLogin,
  submit,
} = useRegisterView();
</script>

<template>
  <section class="page-grid auth-layout auth-layout--centered">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
          <p class="help-text">Create an account to keep ownership of your links.</p>
        </div>

        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Username</span>
            <InputText
              v-model="form.username"
              type="text"
              placeholder="Your username"
              autocomplete="username"
              fluid
            />
          </label>
          <label class="form-field">
            <span class="field-label">Email</span>
            <InputText
              v-model="form.email"
              type="email"
              placeholder="you@example.com"
              autocomplete="email"
              fluid
            />
          </label>
          <label class="form-field">
            <span class="field-label">Password</span>
            <Password
              v-model="form.password"
              :feedback="false"
              toggle-mask
              placeholder="Your password"
              :autocomplete="passwordAutocomplete"
              fluid
            />
          </label>

          <div class="actions auth-primary-actions">
            <Button
              type="submit"
              :label="busy ? 'Working...' : submitLabel"
              icon="pi pi-lock"
              :disabled="busy"
            />
            <Button
              type="button"
              label="GitHub"
              icon="pi pi-github"
              severity="secondary"
              @click="openGithubLogin"
            />
          </div>

          <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
            <span class="status-dot"></span>
            {{ errorMessage }}
          </p>
          <div class="auth-switch">
            <span class="footnote">Already have an account?</span>
            <RouterLink to="/auth/signin" class="auth-inline-link">Sign in</RouterLink>
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
