import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AccountView from '@/features/account/pages/AccountView.vue';
import SecurityView from '@/features/account/pages/SecurityView.vue';

const mocks = vi.hoisted(() => ({
  getAccountProfileMock: vi.fn(),
  changePasswordMock: vi.fn(),
  routerLinkStub: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
}));

vi.mock('@/features/account/repositories/account.repository', () => ({
  getAccountProfile: mocks.getAccountProfileMock,
  changePassword: mocks.changePasswordMock,
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  authState: { currentUser: { username: 'alice', role: 'USER' } },
}));

function mountView(component = AccountView) {
  return mount(component, {
    global: {
      stubs: {
        RouterLink: mocks.routerLinkStub,
      },
    },
  });
}

describe('AccountView', () => {
  it('renders account details and linked providers', async () => {
    mocks.getAccountProfileMock.mockResolvedValue({
      username: 'alice',
      role: 'USER',
      email: 'alice@example.com',
      emailVerified: true,
      createdAt: '2026-05-30T10:00:00Z',
      lastLoginAt: '2026-05-30T12:00:00Z',
      socialIdentities: [{ provider: 'GITHUB', providerLogin: 'alice-github' }],
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain('alice@example.com');
    expect(wrapper.text()).toContain('GITHUB');
    expect(wrapper.text()).toContain('alice-github');
    expect(wrapper.text()).not.toContain('Change password');
  });

  it('submits a password change request from the security page', async () => {
    mocks.getAccountProfileMock.mockResolvedValue({
      username: 'alice',
      role: 'USER',
      email: 'alice@example.com',
      emailVerified: true,
      createdAt: '2026-05-30T10:00:00Z',
      lastLoginAt: '2026-05-30T12:00:00Z',
      socialIdentities: [],
    });
    mocks.changePasswordMock.mockResolvedValue(undefined);

    const wrapper = mountView(SecurityView);
    await flushPromises();

    await wrapper.get('input[placeholder="Current password"]').setValue('Oldpass1');
    await wrapper.get('input[placeholder="New password"]').setValue('Newpass1');
    await wrapper.get('input[placeholder="Confirm new password"]').setValue('Newpass1');
    await wrapper.get('form').trigger('submit.prevent');
    await flushPromises();

    expect(mocks.changePasswordMock).toHaveBeenCalledWith({
      currentPassword: 'Oldpass1',
      newPassword: 'Newpass1',
    });
    expect(wrapper.text()).toContain('Password updated.');
  });
});
