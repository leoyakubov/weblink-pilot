import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { completeGithubLogin } from '@/account/AuthApi';
import { applyAuthResponse } from '@/account/AuthSession';
import type { AuthResponse } from '@/shared/types/api';

export function useGithubLoginCompleteView() {
  /* global URLSearchParams */

  const router = useRouter();
  const busy = ref(true);
  const errorMessage = ref('');

  function readTicket() {
    const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : '';
    const hashParams = new URLSearchParams(hash);
    return (
      hashParams.get('ticket') ?? new URLSearchParams(window.location.search).get('ticket') ?? ''
    );
  }

  function readErrorCode() {
    const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : '';
    const hashParams = new URLSearchParams(hash);
    return (
      hashParams.get('error') ?? new URLSearchParams(window.location.search).get('error') ?? ''
    );
  }

  function formatGithubError(errorCode: string) {
    if (errorCode === 'github_not_configured') {
      return 'GitHub sign-in is not configured for this environment. Use username and password, or add GitHub OAuth credentials.';
    }

    return 'GitHub sign-in could not be completed.';
  }

  function postLoginMessage(response: AuthResponse) {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        {
          type: 'weblinkpilot:github-login',
          response,
        },
        window.location.origin,
      );
      window.close();
      return true;
    }

    return false;
  }

  async function finishLogin() {
    try {
      const oauthError = readErrorCode();
      if (oauthError) {
        throw new Error(formatGithubError(oauthError));
      }

      const ticket = readTicket();
      if (!ticket) {
        throw new Error('Missing GitHub login ticket.');
      }

      const response = await completeGithubLogin({ ticket });
      if (!postLoginMessage(response)) {
        applyAuthResponse(response, `Signed in as ${response.username}`);
        await router.replace('/');
      }
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'GitHub login failed.';
    } finally {
      busy.value = false;
    }
  }

  onMounted(() => {
    void finishLogin();
  });
  return {
    busy,
    errorMessage,
  };
}
