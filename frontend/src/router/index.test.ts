import { beforeEach, describe, expect, it, vi } from 'vitest';
import router from '@/router';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: null as null | { username: string; role: string },
    ready: true,
  },
  bootstrapAuthMock: vi.fn(async () => {
    mocks.authState.ready = true;
  }),
}));

vi.mock('@/account/AuthSession', () => ({
  authState: mocks.authState,
  bootstrapAuth: mocks.bootstrapAuthMock,
}));

beforeEach(async () => {
  vi.stubGlobal('scrollTo', vi.fn());
  mocks.authState.currentUser = null;
  mocks.authState.ready = true;
  mocks.bootstrapAuthMock.mockClear();
  await router.replace('/');
});

describe('router', () => {
  it('exposes the expected routes', () => {
    const names = router.getRoutes().map((route) => route.name);

    expect(names).toEqual(
      expect.arrayContaining([
        'home',
        'about',
        'settings-reset',
        'signin',
        'signup',
        'link',
        'analytics',
        'analytics-detail',
        'links',
        'account',
        'monitoring',
        'admin-users',
      ]),
    );
  });

  it('redirects non-admin users away from monitoring', async () => {
    mocks.authState.currentUser = { username: 'user', role: 'USER' };

    await router.push('/monitoring');

    expect(router.currentRoute.value.name).toBe('home');
    expect(mocks.bootstrapAuthMock).not.toHaveBeenCalled();
  });

  it('redirects non-admin users away from admin users', async () => {
    mocks.authState.currentUser = { username: 'user', role: 'USER' };

    await router.push('/admin/users');

    expect(router.currentRoute.value.name).toBe('home');
  });

  it('allows admins to open monitoring', async () => {
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };

    await router.push('/monitoring');

    expect(router.currentRoute.value.name).toBe('monitoring');
  });

  it('allows admins to open users', async () => {
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };

    await router.push('/admin/users');

    expect(router.currentRoute.value.name).toBe('admin-users');
  });
});
