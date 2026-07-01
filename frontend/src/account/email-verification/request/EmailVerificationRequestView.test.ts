import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import EmailVerificationRequestView from '@/account/email-verification/request/EmailVerificationRequestView.vue';

const mocks = vi.hoisted(() => ({
  requestEmailVerificationMock: vi.fn(),
}));

vi.mock('@/account/AuthApi', () => ({
  ApiRequestError: class ApiRequestError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
  requestEmailVerification: mocks.requestEmailVerificationMock,
}));

describe('EmailVerificationRequestView', () => {
  it('requests a verification email', async () => {
    mocks.requestEmailVerificationMock.mockResolvedValue({
      previewLink: 'http://localhost:8081/auth/verify-email?token=demo-token',
    });

    const wrapper = mount(EmailVerificationRequestView, {
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

    expect(mocks.requestEmailVerificationMock).toHaveBeenCalledWith({
      email: 'alice@example.com',
    });
    expect(wrapper.text()).toContain('Demo email ready');
  });
});
