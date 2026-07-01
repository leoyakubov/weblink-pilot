import { createAccountAccessState } from '@/account/AccountAccess';

export function useLoginView() {
  return createAccountAccessState('login');
}
