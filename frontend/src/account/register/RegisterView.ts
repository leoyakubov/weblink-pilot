import { createAccountAccessState } from '@/account/AccountAccess';

export function useRegisterView() {
  return createAccountAccessState('register');
}
