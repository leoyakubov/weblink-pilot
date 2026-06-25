import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiRequestError } from '@/shared/services/http';
import AuthView from '@/features/auth/pages/AuthView.vue';

const mocks = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  routerReplaceMock: vi.fn(),
  authenticateMock: vi.fn(),
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  routeState: {
    query: {} as Record<string, string>,
  },
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => mocks.routeState,
  useRouter: () => ({
    push: mocks.routerPushMock,
    replace: mocks.routerReplaceMock,
  }),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
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
  beforeEach(() => {
    mocks.routerPushMock.mockClear();
    mocks.routerReplaceMock.mockClear();
    mocks.routeState.query = {};
  });

  it('logs in and routes home on success', async () => {
    mocks.authenticateMock.mockResolvedValue({
      token: 'jwt-token',
      username: 'user',
      role: 'USER',
    });

    const wrapper = mountAuth('login');
    await wrapper.get('input[placeholder="Your username"]').setValue('user');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
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

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="you@example.com"]').setValue('user1@example.com');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('This username already exists.');
  });

  it('shows a signup confirmation modal instead of signing the user in', async () => {
    mocks.authenticateMock.mockResolvedValue(undefined);

    const wrapper = mountAuth('register');
    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="you@example.com"]').setValue('user1@example.com');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Account created');
    expect(wrapper.text()).toContain('Check your email to verify your account before signing in.');
    expect(mocks.routerPushMock).not.toHaveBeenCalled();
  });

  it('shows a verification modal when login is blocked by a 403', async () => {
    mocks.authenticateMock.mockRejectedValue(
      new ApiRequestError(
        'Please verify your email address before signing in',
        403,
        'EMAIL_NOT_VERIFIED',
      ),
    );

    const wrapper = mountAuth('login');
    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Email not verified');
    expect(wrapper.text()).toContain('Resend verification');
  });

  it('shows the verified email modal when redirected from confirmation', async () => {
    mocks.routeState.query = { verified: '1' };

    const wrapper = mountAuth('login');
    await flushPromises();

    expect(wrapper.text()).toContain('Email verified');
    expect(wrapper.text()).toContain('Your email has been verified. You can sign in now.');
    expect(mocks.routerReplaceMock).toHaveBeenCalled();
  });

  it('requires email in register mode', async () => {
    const wrapper = mountAuth('register');

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Email is required.');
  });

  it('opens the GitHub popup from the login form', async () => {
    const focusMock = vi.fn();
    const openMock = vi.spyOn(window, 'open').mockReturnValue({
      focus: focusMock,
      closed: false,
    } as unknown as Window);

    const wrapper = mountAuth('login');
    await wrapper.get('button[aria-label="Continue with GitHub"]').trigger('click');

    expect(openMock).toHaveBeenCalledWith(
      expect.stringContaining('/auth/oauth2/github/start'),
      'weblinkpilot-github-login',
      'popup,width=560,height=720',
    );
    expect(focusMock).toHaveBeenCalled();

    openMock.mockRestore();
  });
});
