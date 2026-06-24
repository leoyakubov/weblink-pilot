import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App.vue';

const mocks = vi.hoisted(() => ({
  routeState: {
    name: 'home' as string | symbol | undefined,
    path: '/',
  },
  authState: {
    currentUser: null as null | { username: string; role: string },
    sessionNotice: '',
  },
  bootstrapAuthMock: vi.fn(),
  signOutMock: vi.fn(),
  routerPushMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  RouterView: {
    template: '<div data-test="router-view"></div>',
  },
  useRoute: () => mocks.routeState,
  useRouter: () => ({
    push: mocks.routerPushMock,
  }),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  authState: mocks.authState,
  bootstrapAuth: mocks.bootstrapAuthMock,
  isAdminUser: () =>
    Boolean(mocks.authState.currentUser && mocks.authState.currentUser.role === 'ADMIN'),
  signOut: mocks.signOutMock,
}));

describe('App', () => {
  beforeEach(() => {
    mocks.bootstrapAuthMock.mockClear();
    mocks.signOutMock.mockClear();
    mocks.routerPushMock.mockClear();
    mocks.authState.currentUser = null;
    mocks.authState.sessionNotice = '';
    mocks.routeState.name = 'home';
    mocks.routeState.path = '/';
  });

  it('renders the logged-out home shell', () => {
    const wrapper = mount(App);

    expect(mocks.bootstrapAuthMock).toHaveBeenCalled();
    expect(wrapper.text()).toContain('Home');
    expect(wrapper.text()).toContain('Dashboard');
    expect(wrapper.text()).toContain('History');
    expect(wrapper.text()).toContain('About');
    expect(wrapper.text()).toContain('Log in');
    expect(wrapper.text()).toContain('Sign up');
    expect(wrapper.text()).toContain('Personal short links, QR codes, and click history');
    expect(wrapper.text()).not.toContain('Monitoring');
  });

  it('renders the dashboard section for admins', async () => {
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };
    mocks.routeState.name = 'dashboard';
    mocks.routeState.path = '/dashboard';

    const wrapper = mount(App);

    expect(wrapper.text()).toContain('Monitoring');
    expect(wrapper.text()).toContain('admin');
    expect(wrapper.text()).toContain('Sign out');

    await wrapper.get('button[aria-label="Open account menu"]').trigger('click');
    expect(wrapper.text()).toContain('Profile');
    expect(wrapper.text()).toContain('Security');

    await wrapper.get('button.sign-out-button').trigger('click');
    expect(mocks.signOutMock).toHaveBeenCalled();
    expect(mocks.routerPushMock).toHaveBeenCalledWith('/');
  });
});
