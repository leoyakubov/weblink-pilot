import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthResponse, UserProfileResponse } from '@/types'

const mocks = vi.hoisted(() => ({
  getCurrentUserMock: vi.fn(),
  loginMock: vi.fn(),
  registerMock: vi.fn(),
  loadSettingsMock: vi.fn(),
  saveSettingsMock: vi.fn(),
}))

const settingsState = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  authToken: '',
}

vi.mock('@/lib/api', () => ({
  getCurrentUser: mocks.getCurrentUserMock,
  login: mocks.loginMock,
  register: mocks.registerMock,
}))

vi.mock('@/lib/settings', () => ({
  loadSettings: mocks.loadSettingsMock,
  saveSettings: mocks.saveSettingsMock,
}))

async function loadAuthModule() {
  vi.resetModules()
  return import('./auth')
}

beforeEach(() => {
  vi.clearAllMocks()
  settingsState.apiBaseUrl = 'http://localhost:8080/api/v1'
  settingsState.authToken = ''
  mocks.loadSettingsMock.mockImplementation(() => ({ ...settingsState }))
  mocks.saveSettingsMock.mockImplementation((settings: typeof settingsState) => {
    settingsState.apiBaseUrl = settings.apiBaseUrl
    settingsState.authToken = settings.authToken
  })
})

describe('auth helpers', () => {
  it('bootstraps the current user when a token is present', async () => {
    const profile: UserProfileResponse = { username: 'admin', role: 'ADMIN' }
    settingsState.authToken = 'jwt-token'
    mocks.getCurrentUserMock.mockResolvedValue(profile)

    const auth = await loadAuthModule()
    await auth.bootstrapAuth()

    expect(mocks.getCurrentUserMock).toHaveBeenCalledWith({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      authToken: 'jwt-token',
    })
    expect(auth.authState.currentUser).toEqual(profile)
    expect(auth.authState.ready).toBe(true)
    expect(auth.authState.loading).toBe(false)
  })

  it('authenticates, saves the token, and signs out again', async () => {
    const authResponse: AuthResponse = {
      token: 'new-token',
      username: 'user',
      role: 'USER',
    }
    mocks.loginMock.mockResolvedValue(authResponse)

    const auth = await loadAuthModule()

    await auth.authenticate('login', { username: 'user', password: 'user123' })
    expect(mocks.loginMock).toHaveBeenCalledWith(
      { username: 'user', password: 'user123' },
      {
        apiBaseUrl: 'http://localhost:8080/api/v1',
        authToken: 'new-token',
      },
    )
    expect(settingsState.authToken).toBe('new-token')
    expect(auth.authState.currentUser).toEqual({
      username: 'user',
      role: 'USER',
    })
    expect(auth.isAdminUser()).toBe(false)

    auth.signOut()
    expect(settingsState.authToken).toBe('')
    expect(auth.authState.currentUser).toBeNull()
    expect(auth.authState.sessionNotice).toContain('Signed out')
  })
})
