import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { confirmEmailVerification } from '@/account/AuthApi';

export function useEmailVerificationConfirmView() {
  const route = useRoute();
  const router = useRouter();
  const busy = ref(true);
  const errorMessage = ref('');

  const title = computed(() => 'Confirm email');

  function getToken() {
    const token = route.query.token;
    return typeof token === 'string' ? token : '';
  }

  function formatError(error: unknown): string {
    if (error instanceof ApiRequestError && error.status === 401) {
      return 'The verification link is invalid or expired.';
    }

    if (error instanceof Error && error.message) {
      return error.message;
    }

    return 'Unable to verify email';
  }

  async function confirm() {
    busy.value = true;
    errorMessage.value = '';

    try {
      const token = getToken();
      if (!token) {
        throw new Error('Verification token is required.');
      }

      await confirmEmailVerification({ token });
      await router.replace({ path: '/auth/signin', query: { verified: '1' } });
    } catch (error) {
      errorMessage.value = formatError(error);
    } finally {
      busy.value = false;
    }
  }

  onMounted(() => {
    void confirm();
  });
  return {
    busy,
    errorMessage,
    title,
  };
}
