import { computed, onMounted, reactive, ref } from 'vue';
import { changePassword } from '@/account/AccountApi';
import { createAccountProfileState } from '@/account/AccountProfileState';
import { ApiRequestError } from '@/shared/services/http';

export function useAccountView() {
  const { account, busy, errorMessage, linkedAccounts, loadAccount } = createAccountProfileState();
  const saving = ref(false);
  const passwordErrorMessage = ref('');
  const successMessage = ref('');
  const hasGithubIdentity = computed(() =>
    linkedAccounts.value.some((identity) => identity.provider.toUpperCase() === 'GITHUB'),
  );
  const form = reactive({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)\S{6,}$/;

  function formatDateTime(value: string | null | undefined) {
    if (!value) {
      return 'Never';
    }

    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  function formatProvider(provider: string) {
    if (provider.toUpperCase() === 'GITHUB') {
      return 'GitHub';
    }

    return provider.toLowerCase().replace(/(^|[-_\s])\w/g, (match) => match.toUpperCase());
  }

  function resetPasswordMessages() {
    passwordErrorMessage.value = '';
    successMessage.value = '';
  }

  function validatePassword(): string {
    if (!form.currentPassword.trim()) {
      return 'Enter your current password.';
    }
    if (!form.newPassword.trim()) {
      return 'Enter a new password.';
    }
    if (!passwordPattern.test(form.newPassword)) {
      return 'Password must use at least 6 characters, including 1 letter and 1 number.';
    }
    if (form.newPassword !== form.confirmPassword) {
      return 'Passwords do not match.';
    }
    return '';
  }

  function formatChangeError(error: unknown): string {
    if (error instanceof ApiRequestError) {
      if (error.status === 401) {
        return 'Current password is incorrect.';
      }
      if (error.status === 400) {
        return error.message || 'Unable to update password.';
      }
    }
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return 'Unable to update password.';
  }

  async function submitPasswordChange() {
    saving.value = true;
    resetPasswordMessages();
    try {
      const validationError = validatePassword();
      if (validationError) {
        passwordErrorMessage.value = validationError;
        return;
      }

      await changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });

      form.currentPassword = '';
      form.newPassword = '';
      form.confirmPassword = '';
      successMessage.value = 'Password updated. Other refresh sessions were revoked.';
    } catch (error) {
      passwordErrorMessage.value = formatChangeError(error);
    } finally {
      saving.value = false;
    }
  }

  onMounted(() => {
    void loadAccount();
  });
  return {
    account,
    busy,
    errorMessage,
    linkedAccounts,
    saving,
    passwordErrorMessage,
    successMessage,
    hasGithubIdentity,
    form,
    formatDateTime,
    formatProvider,
    submitPasswordChange,
  };
}
