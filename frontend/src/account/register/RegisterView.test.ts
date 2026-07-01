import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiRequestError } from '@/shared/services/http';
import RegisterView from './RegisterView.vue';

const mocks = vi.hoisted(() => ({
  routerPushMock: vi.fn(),
  routerReplaceMock: vi.fn(),
  authenticateMock: vi.fn(),
  authState: {
    currentUser: null as null | { username: string; role: string },
  },
  routeState: {
    fullPath: '/auth/signup',
    path: '/auth/signup',
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

describe('RegisterView', () => {
  beforeEach(() => {
    mocks.routerPushMock.mockClear();
    mocks.routerReplaceMock.mockClear();
    mocks.authenticateMock.mockReset();
    mocks.routeState.fullPath = '/auth/signup';
    mocks.routeState.path = '/auth/signup';
    mocks.routeState.query = {};
  });

  it('shows register validation and duplicate user errors', async () => {
    mocks.authenticateMock.mockRejectedValue(new ApiRequestError('Conflict', 409));

    const wrapper = mount(RegisterView);

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="you@example.com"]').setValue('user1@example.com');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('This username already exists.');
  });

  it('shows a signup confirmation modal instead of signing the user in', async () => {
    mocks.authenticateMock.mockResolvedValue(undefined);

    const wrapper = mount(RegisterView);
    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="you@example.com"]').setValue('user1@example.com');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Account created');
    expect(wrapper.text()).toContain('Verify your email before signing in.');
    expect(mocks.routerPushMock).not.toHaveBeenCalled();
  });

  it('requires email in register mode', async () => {
    const wrapper = mount(RegisterView);

    await wrapper.get('input[placeholder="Your username"]').setValue('user1');
    await wrapper.get('input[placeholder="Your password"]').setValue('user123');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(wrapper.text()).toContain('Email is required.');
  });
});
