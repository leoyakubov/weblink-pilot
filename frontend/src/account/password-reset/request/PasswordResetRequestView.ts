import { computed, reactive, ref } from 'vue';
import { ApiRequestError } from '@/shared/services/http';
import { requestPasswordReset } from '@/account/AuthApi';

export function usePasswordResetRequestView() {
  const form = reactive({
    email: '',
  });

  const busy = ref(false);
  const errorMessage = ref('');
  const successMessage = ref('');
  const noticeVisible = ref(false);
  const noticeTitle = ref('');
  const noticeMessage = ref('');
  const noticeActionLabel = ref('');
  const noticeActionHandler = ref<null | (() => unknown)>(null);

  const title = computed(() => 'Reset password');

  function validateForm(): string {
    if (!form.email.trim()) {
      return 'Email is required.';
    }
    return '';
  }

  function formatError(error: unknown): string {
    if (error instanceof ApiRequestError) {
      if (error.status === 400) {
        return 'Enter a valid email address.';
      }
      return 'Unable to request a password reset. Please try again.';
    }
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return 'Unable to request a password reset.';
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

      const response = await requestPasswordReset({ email: form.email.trim() });
      if (response.previewLink) {
        openNotice(
          'Demo email ready',
          'Open the reset link to choose a new password.',
          'Open reset link',
          () => {
            const popup = window.open(response.previewLink ?? '', '_blank', 'noopener');
            popup?.focus();
          },
        );
        return;
      }

      successMessage.value = 'If the email exists, a reset link was sent. Check your inbox.';
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
    noticeVisible,
    noticeTitle,
    noticeMessage,
    noticeActionLabel,
    title,
    closeNotice,
    handleNoticeAction,
    submit,
  };
}
