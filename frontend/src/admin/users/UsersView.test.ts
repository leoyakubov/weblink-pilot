import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import UsersView from '@/admin/users/UsersView.vue';

const mocks = vi.hoisted(() => ({
  listAdminUsersPageMock: vi.fn(),
}));

vi.mock('@/admin/AdminApi', () => ({
  listAdminUsersPage: mocks.listAdminUsersPageMock,
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
    mocks.listAdminUsersPageMock.mockResolvedValue({
      content: [
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
      ],
      page: 0,
      size: 10,
      totalElements: 12,
      totalPages: 2,
      first: true,
      last: false,
    });
  });

  it('renders read-only admin users', async () => {
    const wrapper = mount(UsersView);
    await flushPromises();

    expect(wrapper.text()).toContain('Users');
    expect(wrapper.text()).toContain('User directory');
    expect(wrapper.text()).toContain('Total users');
    expect(wrapper.text()).toContain('Admins on page');
    expect(wrapper.text()).toContain('Active on page');
    expect(wrapper.text()).toContain('Page 1 of 2');
    expect(wrapper.text()).toContain('admin@example.com');
    expect(wrapper.text()).toContain('user@example.com');
    expect(wrapper.text()).toContain('Email pending');
    expect(wrapper.text()).not.toContain('email verified');
    expect(wrapper.text()).toContain('Never');
    expect(mocks.listAdminUsersPageMock).toHaveBeenCalledWith(0, 10, expect.any(Object));
  });

  it('loads the next page from pagination controls', async () => {
    const wrapper = mount(UsersView);
    await flushPromises();

    const nextButton = wrapper.findAll('button').find((button) => button.text().includes('Next'));
    expect(nextButton).toBeDefined();
    await nextButton?.trigger('click');
    await flushPromises();

    expect(mocks.listAdminUsersPageMock).toHaveBeenLastCalledWith(1, 10, expect.any(Object));
  });
});
