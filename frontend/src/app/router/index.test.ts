import { beforeEach, describe, expect, it, vi } from 'vitest';
import router from '@/app/router';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: null as null | { username: string; role: string },
    ready: true,
  },
  bootstrapAuthMock: vi.fn(async () => {
    mocks.authState.ready = true;
  }),
}));

vi.mock('@/features/auth/services/auth.service', () => ({
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
        'signin',
        'signup',
        'link',
        'dashboard',
        'history',
        'monitoring',
      ]),
    );
  });

  it('redirects non-admin users away from monitoring', async () => {
    mocks.authState.currentUser = { username: 'user', role: 'USER' };

    await router.push('/monitoring');

    expect(router.currentRoute.value.name).toBe('home');
    expect(mocks.bootstrapAuthMock).not.toHaveBeenCalled();
  });

  it('allows admins to open monitoring', async () => {
    mocks.authState.currentUser = { username: 'admin', role: 'ADMIN' };

    await router.push('/monitoring');

    expect(router.currentRoute.value.name).toBe('monitoring');
  });
});
