import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import PasswordResetRequestView from '@/features/auth/pages/PasswordResetRequestView.vue';

const mocks = vi.hoisted(() => ({
  requestPasswordResetMock: vi.fn(),
}));

vi.mock('@/features/auth/repositories/auth.repository', () => ({
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
    mocks.requestPasswordResetMock.mockResolvedValue({
      previewLink: 'http://localhost:8081/auth/reset-password?token=demo-token',
    });

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
    expect(wrapper.text()).toContain('Demo email ready');
  });
});
