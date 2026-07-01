import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiRequestError } from '@/shared/services/http';
import LoginView from './LoginView.vue';

const mocks = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  routerReplaceMock: vi.fn(),
  authenticateMock: vi.fn(),
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  routeState: {
    fullPath: '/auth/signin',
    path: '/auth/signin',
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

vi.mock('@/account/AuthSession', () => ({
  authenticate: mocks.authenticateMock,
  authState: mocks.authState,
}));

describe('LoginView', () => {
  beforeEach(() => {
    mocks.routerPushMock.mockClear();
    mocks.routerReplaceMock.mockClear();
    mocks.authenticateMock.mockReset();
    mocks.routeState.fullPath = '/auth/signin';
    mocks.routeState.path = '/auth/signin';
    mocks.routeState.query = {};
  });

  it('logs in and routes home on success', async () => {
    mocks.authenticateMock.mockResolvedValue({
      token: 'jwt-token',
      username: 'user',
      role: 'USER',
    });

    const wrapper = mount(LoginView);
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

  it('shows a verification modal when login is blocked by a 403', async () => {
    mocks.authenticateMock.mockRejectedValue(
      new ApiRequestError(
        'Please verify your email address before signing in',
        403,
        'EMAIL_NOT_VERIFIED',
      ),
    );

    const wrapper = mount(LoginView);
    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Email not verified');
    expect(wrapper.text()).toContain('Resend verification');
  });

  it('shows the verified email modal when redirected from confirmation', async () => {
    mocks.routeState.query = { verified: '1' };

    const wrapper = mount(LoginView);
    await flushPromises();

    expect(wrapper.text()).toContain('Email verified');
    expect(wrapper.text()).toContain('Your email has been verified. You can sign in now.');
    expect(mocks.routerReplaceMock).toHaveBeenCalled();
  });

  it('opens the GitHub popup from the login form', async () => {
    const focusMock = vi.fn();
    const openMock = vi.spyOn(window, 'open').mockReturnValue({
      focus: focusMock,
      closed: false,
    } as unknown as Window);

    const wrapper = mount(LoginView);
    await wrapper.get('button[aria-label="GitHub"]').trigger('click');

    expect(openMock).toHaveBeenCalledWith(
      expect.stringContaining('/auth/oauth2/github/start'),
      'weblinkpilot-github-login',
      'popup,width=560,height=720',
    );
    expect(focusMock).toHaveBeenCalled();

    openMock.mockRestore();
  });
});
