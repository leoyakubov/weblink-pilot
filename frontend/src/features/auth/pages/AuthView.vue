<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import { ApiRequestError, buildApiBaseUrl } from '@/shared/services/http';
import { authenticate, authState } from '@/features/auth/services/auth.service';
import type { AuthCredentialsRequest } from '@/shared/types/api';

const props = defineProps<{
  mode: 'login' | 'register';
}>();

const router = useRouter();
const form = reactive<AuthCredentialsRequest>({
  username: '',
  password: '',
  email: '',
});
const busy = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const showPassword = ref(false);
const usernamePattern = /^(?=.*[A-Za-z])[A-Za-z0-9]{4,}$/;
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

const title = computed(() => (props.mode === 'login' ? 'Sign in' : 'Sign up'));
const submitLabel = computed(() => (props.mode === 'login' ? 'Sign in' : 'Create account'));
const switchPath = computed(() => (props.mode === 'login' ? '/auth/signup' : '/auth/signin'));
const passwordAutocomplete = computed(() =>
  props.mode === 'register' ? 'new-password' : 'current-password',
);

function resetMessages() {
  errorMessage.value = '';
  successMessage.value = '';
  showPassword.value = false;
}

watch(
  () => props.mode,
  () => {
    resetMessages();
  },
  { immediate: true },
);

function validateForm(): string {
  const username = form.username.trim();
  const password = form.password.trim();

  if (!username && !password) {
    return 'Enter both username and password.';
  }

  if (props.mode === 'register') {
    if (!username) {
      return 'Username must use at least 4 symbols.';
    }

    if (!usernamePattern.test(username)) {
      return 'Username must use at least 4 symbols.';
    }

    if (!password) {
      return 'Password must use at least 6 characters, including 1 letter and 1 number.';
    }

    if (!passwordPattern.test(password)) {
      return 'Password must use at least 6 characters, including 1 letter and 1 number.';
    }

    if (!form.email?.trim()) {
      return 'Email is required.';
    }
  } else if (!username || !password) {
    return 'Enter both username and password.';
  }

  return '';
}

function formatAuthError(error: unknown): string {
  if (error instanceof ApiRequestError) {
    if (error.code === 'EMAIL_NOT_VERIFIED') {
      return 'Please verify your email address before signing in.';
    }

    if (error.status === 401) {
      return 'Incorrect username or password.';
    }

    if (error.status === 409) {
      return 'This username already exists.';
    }

    if (error.status === 403) {
      return 'This account is disabled.';
    }

    return 'Authentication failed. Please try again.';
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Authentication failed';
}

function openGithubLogin() {
  const popup = window.open(
    buildApiBaseUrl('/auth/oauth2/github/start'),
    'weblinkpilot-github-login',
    'popup,width=560,height=720',
  );

  if (!popup) {
    errorMessage.value = 'Allow pop-ups to continue with GitHub sign-in.';
    return;
  }

  popup.focus();
}

async function submit() {
  busy.value = true;
  resetMessages();

  try {
    const validationError = validateForm();
    if (validationError) {
      errorMessage.value = validationError;
      return;
    }

    const request =
      props.mode === 'login'
        ? {
            username: form.username,
            password: form.password,
          }
        : form;

    await authenticate(props.mode, request);
    successMessage.value =
      props.mode === 'login'
        ? `Signed in as ${authState.currentUser?.username}`
        : `Created ${authState.currentUser?.username}, signed in, and sent a verification email`;
    await router.push('/');
  } catch (error) {
    errorMessage.value = formatAuthError(error);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="page-grid two-col auth-layout">
    <article class="card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account access</p>
          <h3 class="panel-title">Move between guest mode and saved ownership.</h3>
        </div>

        <p class="hero-note">
          Use a password for the local account flow, or switch to GitHub sign-in when you want to
          keep ownership tied to an external identity.
        </p>

        <div class="grid-2">
          <div class="list-item">
            <strong>Fast sign-in</strong>
            <p>Use your existing account to get back to owned links and analytics.</p>
          </div>
          <div class="list-item">
            <strong>Recovery ready</strong>
            <p>Reset email and verification flows stay available from the same page.</p>
          </div>
        </div>

        <div class="list-item">
          <strong>Current mode</strong>
          <p>{{ props.mode === 'login' ? 'Sign in to your account' : 'Create a new account' }}</p>
        </div>
      </div>
    </article>

    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
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
          <label v-if="props.mode === 'register'" class="form-field">
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
              :placeholder="'Enter your password'"
              :autocomplete="passwordAutocomplete"
              fluid
            />
          </label>

          <div class="actions">
            <Button
              type="submit"
              :label="busy ? 'Working...' : submitLabel"
              icon="pi pi-lock"
              :disabled="busy"
            />
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
            <span class="footnote">
              {{ props.mode === 'login' ? 'Need an account?' : 'Already have an account?' }}
            </span>
            <RouterLink :to="switchPath">
              <Button
                :label="props.mode === 'login' ? 'Sign up' : 'Sign in'"
                severity="secondary"
                variant="outlined"
                size="small"
              />
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Forgot your password?</span>
            <RouterLink to="/auth/forgot-password">
              <Button
                label="Reset password"
                severity="secondary"
                variant="outlined"
                size="small"
              />
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Need a verification email?</span>
            <RouterLink to="/auth/verify-email/request">
              <Button
                label="Resend verification"
                severity="secondary"
                variant="outlined"
                size="small"
              />
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Prefer GitHub?</span>
            <Button
              type="button"
              label="Continue with GitHub"
              icon="pi pi-github"
              severity="secondary"
              variant="outlined"
              size="small"
              @click="openGithubLogin"
            />
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
