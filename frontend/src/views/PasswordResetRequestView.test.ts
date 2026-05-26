import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import PasswordResetRequestView from './PasswordResetRequestView.vue';

const mocks = vi.hoisted(() => ({
  requestPasswordResetMock: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  ApiRequestError: class ApiRequestError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
  requestPasswordReset: mocks.requestPasswordResetMock,
}));

describe('PasswordResetRequestView', () => {
  it('requests a password reset link', async () => {
    mocks.requestPasswordResetMock.mockResolvedValue(undefined);

    const wrapper = mount(PasswordResetRequestView, {
      global: {
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    });

    await wrapper.get('input[placeholder="you@example.com"]').setValue('alice@example.com');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.requestPasswordResetMock).toHaveBeenCalledWith({
      email: 'alice@example.com',
    });
    expect(wrapper.text()).toContain('reset link was sent');
  });
});
