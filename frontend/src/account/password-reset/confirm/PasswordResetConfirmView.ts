import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { confirmPasswordReset } from '@/account/AuthApi';

export function usePasswordResetConfirmView() {
  const route = useRoute();
  const router = useRouter();

  const form = reactive({
    token: String(route.query.token ?? ''),
    password: '',
  });

  const busy = ref(false);
  const errorMessage = ref('');
  const successMessage = ref('');

  const title = computed(() => 'Set new password');
  const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

  function validateForm(): string {
    if (!form.token.trim()) {
      return 'Reset token is required.';
    }
    if (!form.password.trim()) {
      return 'Password is required.';
    }
    if (!passwordPattern.test(form.password.trim())) {
      return 'Password must use at least 6 characters, including 1 letter and 1 number.';
    }
    return '';
  }

  function formatError(error: unknown): string {
    if (error instanceof ApiRequestError) {
      if (error.status === 401 || error.status === 400) {
        return 'This reset link is invalid or expired.';
      }
      return 'Unable to change password. Please try again.';
    }
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return 'Unable to change password.';
  }

  async function submit() {
    busy.value = true;
    errorMessage.value = '';
    successMessage.value = '';

    try {
      const validationError = validateForm();
      if (validationError) {
        errorMessage.value = validationError;
        return;
      }

      await confirmPasswordReset({
        token: form.token.trim(),
        password: form.password.trim(),
      });
      successMessage.value = 'Password updated. You can sign in with the new password now.';
      await router.push('/auth/signin');
    } catch (error) {
      errorMessage.value = formatError(error);
    } finally {
      busy.value = false;
    }
  }
  return {
    form,
    busy,
    errorMessage,
    successMessage,
    title,
    submit,
  };
}
