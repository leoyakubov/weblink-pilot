import { computed, ref } from 'vue';
import { ApiRequestError } from '@/shared/services/http';
import { getAccountProfile } from '@/account/AccountApi';
import type { AccountProfileResponse } from '@/shared/types/api';

export function createAccountProfileState() {
  const busy = ref(true);
  const errorMessage = ref('');
  const account = ref<AccountProfileResponse | null>(null);
  const linkedAccounts = computed(() => account.value?.socialIdentities ?? []);

  async function loadAccount() {
    busy.value = true;
    errorMessage.value = '';

    try {
      account.value = await getAccountProfile();
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        errorMessage.value = 'Please sign in again to view your account settings.';
      } else {
        errorMessage.value =
          error instanceof Error ? error.message : 'Unable to load account settings.';
      }
    } finally {
      busy.value = false;
    }
  }

  return {
    account,
    busy,
    errorMessage,
    linkedAccounts,
    loadAccount,
  };
}
