import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import EmailVerificationConfirmView from '@/features/auth/pages/EmailVerificationConfirmView.vue';

const mocks = vi.hoisted(() => ({
  confirmEmailVerificationMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({
    query: { token: 'verify-token' },
  }),
}));

vi.mock('@/features/auth/repositories/auth.repository', () => ({
  ApiRequestError: class ApiRequestError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
  confirmEmailVerification: mocks.confirmEmailVerificationMock,
}));

describe('EmailVerificationConfirmView', () => {
  it('confirms an email verification link', async () => {
    mocks.confirmEmailVerificationMock.mockResolvedValue(undefined);

    const wrapper = mount(EmailVerificationConfirmView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    });

    await flushPromises();

    expect(mocks.confirmEmailVerificationMock).toHaveBeenCalledWith({
      token: 'verify-token',
    });
    expect(wrapper.text()).toContain('Your email has been verified');
  });
});
