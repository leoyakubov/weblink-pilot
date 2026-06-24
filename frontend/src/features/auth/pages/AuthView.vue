<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import { ApiRequestError, buildApiBaseUrl } from '@/shared/services/http';
import { authenticate } from '@/features/auth/services/auth.service';
import type { AuthCredentialsRequest } from '@/shared/types/api';
import AuthNoticeModal from '@/features/auth/components/AuthNoticeModal.vue';

const props = defineProps<{
  mode: 'login' | 'register';
}>();

const route = useRoute();
const router = useRouter();
const form = reactive<AuthCredentialsRequest>({
  username: '',
  password: '',
  email: '',
});
const busy = ref(false);
const errorMessage = ref('');
const showPassword = ref(false);
const noticeVisible = ref(false);
const noticeTitle = ref('');
const noticeMessage = ref('');
const noticeActionLabel = ref('');
const noticeActionHandler = ref<null | (() => unknown)>(null);
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
  showPassword.value = false;
}

function openNotice(
  title: string,
  message: string,
  actionLabel?: string,
  actionHandler?: () => unknown,
) {
  noticeTitle.value = title;
  noticeMessage.value = message;
  noticeActionLabel.value = actionLabel ?? '';
  noticeActionHandler.value = actionHandler ?? null;
  noticeVisible.value = true;
}

function closeNotice() {
  noticeVisible.value = false;
}

function handleNoticeAction() {
  const action = noticeActionHandler.value;
  closeNotice();
  if (action) {
    void action();
  }
}

function openPreviewLink(previewLink: string) {
  const popup = window.open(previewLink, '_blank', 'noopener');
  if (popup) {
    popup.focus();
  }
}

watch(
  () => props.mode,
  () => {
    resetMessages();
  },
  { immediate: true },
);

watch(
  () => route.query.verified,
  (verified) => {
    if (props.mode !== 'login' || verified !== '1') {
      return;
    }

    openNotice('Email verified', 'Your email has been verified. You can sign in now.');

    const { verified: _verified, ...remainingQuery } = route.query;
    void router.replace({ path: route.path, query: remainingQuery });
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

    const response = await authenticate(props.mode, request);

    if (props.mode === 'login') {
      await router.push('/');
      return;
    }

    if (response && 'previewLink' in response && response.previewLink) {
      const previewLink = response.previewLink;
      openNotice(
        'Demo email ready',
        'Use the preview link to verify the account and continue the demo flow.',
        'Open verification link',
        () => openPreviewLink(previewLink),
      );
      return;
    }

    openNotice(
      'Account created',
      'Check your email to verify your account before signing in.',
      'Go to sign in',
      () => router.push('/auth/signin'),
    );
  } catch (error) {
    if (
      error instanceof ApiRequestError &&
      (error.status === 403 || error.code === 'EMAIL_NOT_VERIFIED')
    ) {
      openNotice(
        'Email not verified',
        'This account still needs email verification. Open the verification email or request a new link.',
        'Resend verification',
        () => router.push('/auth/verify-email/request'),
      );
      return;
    }

    errorMessage.value = formatAuthError(error);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="page-grid auth-layout auth-layout--centered">
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

          <div class="actions auth-primary-actions">
            <Button
              type="submit"
              :label="busy ? 'Working...' : submitLabel"
              icon="pi pi-lock"
              :disabled="busy"
            />
            <Button
              type="button"
              label="Continue with GitHub"
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
              <Button label="Reset password" severity="secondary" variant="outlined" size="small" />
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
