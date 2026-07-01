import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiRequestError, buildApiBaseUrl } from '@/shared/services/http';
import { authenticate } from '@/account/AuthSession';
import type { AuthCredentialsRequest } from '@/shared/types/api';

export type AccountAccessMode = 'login' | 'register';

export function createAccountAccessState(mode: AccountAccessMode) {
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

  const title = computed(() => (mode === 'login' ? 'Sign in' : 'Sign up'));
  const submitLabel = computed(() => (mode === 'login' ? 'Sign in' : 'Create account'));
  const switchPath = computed(() => (mode === 'login' ? '/auth/signup' : '/auth/signin'));
  const passwordAutocomplete = computed(() =>
    mode === 'register' ? 'new-password' : 'current-password',
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
    () => route.fullPath,
    () => resetMessages(),
    { immediate: true },
  );

  watch(
    () => route.query.verified,
    (verified) => {
      if (mode !== 'login' || verified !== '1') {
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

    if (mode === 'register') {
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
        mode === 'login'
          ? {
              username: form.username,
              password: form.password,
            }
          : form;

      const response = await authenticate(mode, request);

      if (mode === 'login') {
        await router.push('/');
        return;
      }

      if (response && 'previewLink' in response && response.previewLink) {
        const previewLink = response.previewLink;
        openNotice(
          'Demo email ready',
          'Open the preview link to verify the account and continue.',
          'Open verification link',
          () => openPreviewLink(previewLink),
        );
        return;
      }

      openNotice('Account created', 'Verify your email before signing in.', 'Go to sign in', () =>
        router.push('/auth/signin'),
      );
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        (error.status === 403 || error.code === 'EMAIL_NOT_VERIFIED')
      ) {
        openNotice(
          'Email not verified',
          'This account still needs email verification. Open the email link or request a new one.',
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
  return {
    form,
    busy,
    errorMessage,
    showPassword,
    noticeVisible,
    noticeTitle,
    noticeMessage,
    noticeActionLabel,
    title,
    submitLabel,
    switchPath,
    passwordAutocomplete,
    closeNotice,
    handleNoticeAction,
    openGithubLogin,
    submit,
  };
}
