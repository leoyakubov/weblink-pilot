import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import UsersView from '@/admin/users/UsersView.vue';

const mocks = vi.hoisted(() => ({
  listAdminUsersMock: vi.fn(),
}));

vi.mock('@/admin/AdminApi', () => ({
  listAdminUsers: mocks.listAdminUsersMock,
}));

vi.mock('@/shared/services/settings', () => ({
  loadSettings: () => ({
    apiBaseUrl: 'http://localhost:8080/api/v1',
    authToken: 'token',
    refreshToken: 'refresh',
  }),
}));

describe('UsersView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.listAdminUsersMock.mockResolvedValue([
      {
        username: 'admin',
        email: 'admin@example.com',
        role: 'ADMIN',
        enabled: true,
        emailVerified: true,
        createdAt: '2026-06-20T10:00:00Z',
        lastLoginAt: '2026-06-21T10:00:00Z',
      },
      {
        username: 'user',
        email: 'user@example.com',
        role: 'USER',
        enabled: true,
        emailVerified: false,
        createdAt: '2026-06-20T11:00:00Z',
        lastLoginAt: null,
      },
    ]);
  });

  it('renders read-only admin users', async () => {
    const wrapper = mount(UsersView);
    await flushPromises();

    expect(wrapper.text()).toContain('Users');
    expect(wrapper.text()).toContain('User directory');
    expect(wrapper.text()).toContain('Total users');
    expect(wrapper.text()).toContain('Admins');
    expect(wrapper.text()).toContain('admin@example.com');
    expect(wrapper.text()).toContain('user@example.com');
    expect(wrapper.text()).toContain('Email pending');
    expect(wrapper.text()).not.toContain('email verified');
    expect(wrapper.text()).toContain('Never');
    expect(mocks.listAdminUsersMock).toHaveBeenCalledTimes(1);
  });
});
