<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ApiRequestError, buildApiBaseUrl } from '@/lib/api';
import { authenticate, authState } from '@/lib/auth';
import type { AuthCredentialsRequest } from '@/types';

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
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
        </div>

        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Username</span>
            <input
              v-model="form.username"
              class="input"
              type="text"
              placeholder="Your username"
              autocomplete="username"
            />
          </label>
          <label v-if="props.mode === 'register'" class="form-field">
            <span class="field-label">Email</span>
            <input
              v-model="form.email"
              class="input"
              type="email"
              placeholder="you@example.com"
              autocomplete="email"
            />
          </label>
          <label class="form-field">
            <span class="field-label">Password</span>
            <div class="input-row">
              <input
                v-model="form.password"
                class="input"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Enter your password"
                :autocomplete="passwordAutocomplete"
              />
              <button
                class="password-toggle"
                type="button"
                :aria-label="showPassword ? 'Hide password' : 'Show password'"
                :title="showPassword ? 'Hide password' : 'Show password'"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M3 3l18 18M10.5 10.62a2 2 0 1 0 2.88 2.76m-4.21-4.17A4.5 4.5 0 0 1 16.5 12c0 .88-.24 1.7-.66 2.4m-2.02 2.02A4.5 4.5 0 0 1 7.5 12c0-.88.24-1.7.66-2.4M9.9 5.23A10.96 10.96 0 0 1 12 4.5c5.5 0 9.5 7.5 9.5 7.5a19.58 19.58 0 0 1-4.1 5.12M6.84 6.84A19.83 19.83 0 0 0 2.5 12s4 7.5 9.5 7.5c1.26 0 2.47-.23 3.56-.64"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                </svg>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M2.5 12S6.4 4.5 12 4.5 21.5 12 21.5 12 17.6 19.5 12 19.5 2.5 12 2.5 12Z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                  <circle
                    cx="12"
                    cy="12"
                    r="2.75"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.7"
                  />
                </svg>
              </button>
            </div>
          </label>

          <div class="actions">
            <button class="button button-primary" type="submit" :disabled="busy">
              {{ busy ? 'Working...' : submitLabel }}
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
            <span class="footnote">
              {{ props.mode === 'login' ? 'Need an account?' : 'Already have an account?' }}
            </span>
            <RouterLink
              class="button button-secondary button-small auth-link-button"
              :to="switchPath"
            >
              {{ props.mode === 'login' ? 'Sign up' : 'Sign in' }}
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Forgot your password?</span>
            <RouterLink class="button button-secondary button-small auth-link-button" to="/auth/forgot-password">
              Reset password
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Need a verification email?</span>
            <RouterLink
              class="button button-secondary button-small auth-link-button"
              to="/auth/verify-email/request"
            >
              Resend verification
            </RouterLink>
          </div>
          <div v-if="props.mode === 'login'" class="auth-switch">
            <span class="footnote">Prefer GitHub?</span>
            <button class="button button-secondary button-small auth-link-button" type="button" @click="openGithubLogin">
              Continue with GitHub
            </button>
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
