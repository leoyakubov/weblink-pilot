import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import PasswordResetConfirmView from './PasswordResetConfirmView.vue';

const mocks = vi.hoisted(() => ({
  confirmPasswordResetMock: vi.fn(),
  routerPushMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({
    query: { token: 'reset-token' },
  }),
  useRouter: () => ({
    push: mocks.routerPushMock,
  }),
}));

vi.mock('@/lib/api', () => ({
  ApiRequestError: class ApiRequestError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
  confirmPasswordReset: mocks.confirmPasswordResetMock,
}));

describe('PasswordResetConfirmView', () => {
  it('confirms a password reset and routes to sign in', async () => {
    mocks.confirmPasswordResetMock.mockResolvedValue(undefined);

    const wrapper = mount(PasswordResetConfirmView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    });

    await wrapper.get('input[placeholder="Enter a new password"]').setValue('Password1');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.confirmPasswordResetMock).toHaveBeenCalledWith({
      token: 'reset-token',
      password: 'Password1',
    });
    expect(mocks.routerPushMock).toHaveBeenCalledWith('/auth/signin');
  });
});
