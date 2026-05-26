import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { ApiRequestError } from '@/lib/api';
import AuthView from './AuthView.vue';

const mocks = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  authenticateMock: vi.fn(),
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRouter: () => ({
    push: mocks.routerPushMock,
  }),
}));

vi.mock('@/lib/auth', () => ({
  authenticate: mocks.authenticateMock,
  authState: mocks.authState,
}));

function mountAuth(mode: 'login' | 'register') {
  return mount(AuthView, {
    props: { mode },
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>',
        },
      },
    },
  });
}

describe('AuthView', () => {
  it('logs in and routes home on success', async () => {
    mocks.authenticateMock.mockResolvedValue({
      token: 'jwt-token',
      username: 'user',
      role: 'USER',
    });

    const wrapper = mountAuth('login');
    await wrapper.get('input[placeholder="Your username"]').setValue('user');
    await wrapper.get('input[placeholder="Enter your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.authenticateMock).toHaveBeenCalledWith('login', {
      username: 'user',
      password: 'user123',
    });
    expect(mocks.routerPushMock).toHaveBeenCalledWith('/');
  });

  it('shows register validation and duplicate user errors', async () => {
    mocks.authenticateMock.mockRejectedValue(new ApiRequestError('Conflict', 409));

    const wrapper = mountAuth('register');
    await wrapper.get('button[aria-label="Show password"]').trigger('click');
    expect(wrapper.get('input[placeholder="Enter your password"]').element).toHaveProperty(
      'type',
      'text',
    );

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="you@example.com"]').setValue('user1@example.com');
    await wrapper.get('input[placeholder="Enter your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('This username already exists.');
  });

  it('requires email in register mode', async () => {
    const wrapper = mountAuth('register');

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="Enter your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Email is required.');
  });
});
