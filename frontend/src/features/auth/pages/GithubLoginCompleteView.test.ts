import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import GithubLoginCompleteView from '@/features/auth/pages/GithubLoginCompleteView.vue';

const mocks = vi.hoisted(() => ({
  completeGithubLoginMock: vi.fn(),
  applyAuthResponseMock: vi.fn(),
  routerReplaceMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplaceMock,
  }),
}));

vi.mock('@/features/auth/repositories/auth.repository', () => ({
  completeGithubLogin: mocks.completeGithubLoginMock,
}));

vi.mock('@/features/auth/services/auth.service', () => ({
  applyAuthResponse: mocks.applyAuthResponseMock,
}));

describe('GithubLoginCompleteView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState({}, '', '/auth/github/complete');
  });

  it('completes a GitHub ticket and redirects to home when opened directly', async () => {
    window.history.replaceState({}, '', '/auth/github/complete?ticket=github-ticket');
    mocks.completeGithubLoginMock.mockResolvedValue({
      token: 'jwt-token',
      username: 'leonid',
      role: 'USER',
    });

    mount(GithubLoginCompleteView);
    await flushPromises();

    expect(mocks.completeGithubLoginMock).toHaveBeenCalledWith({ ticket: 'github-ticket' });
    expect(mocks.applyAuthResponseMock).toHaveBeenCalledWith(
      { token: 'jwt-token', username: 'leonid', role: 'USER' },
      'Signed in as leonid',
    );
    expect(mocks.routerReplaceMock).toHaveBeenCalledWith('/');
  });

  it('shows a useful message when GitHub OAuth is not configured', async () => {
    window.history.replaceState({}, '', '/auth/github/complete#error=github_not_configured');

    const wrapper = mount(GithubLoginCompleteView);
    await flushPromises();

    expect(mocks.completeGithubLoginMock).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('GitHub sign-in is not configured');
  });
});
