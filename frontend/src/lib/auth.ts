import { reactive } from 'vue'
import { getCurrentUser, login, register } from '@/lib/api'
import { loadSettings, saveSettings } from '@/lib/settings'
import type { AuthCredentialsRequest, AuthResponse, UserProfileResponse } from '@/types'

export const authState = reactive({
  currentUser: null as UserProfileResponse | null,
  ready: false,
  loading: false,
})

let bootstrapPromise: Promise<void> | null = null

export async function bootstrapAuth() {
  if (bootstrapPromise) {
    return bootstrapPromise
  }

  bootstrapPromise = (async () => {
    authState.loading = true
    const settings = loadSettings()

    try {
      if (settings.authToken) {
        authState.currentUser = await getCurrentUser(settings)
      } else {
        authState.currentUser = null
      }
    } catch {
      settings.authToken = ''
      saveSettings(settings)
      authState.currentUser = null
    } finally {
      authState.loading = false
      authState.ready = true
    }
  })()

  return bootstrapPromise
}

export async function authenticate(mode: 'login' | 'register', request: AuthCredentialsRequest): Promise<AuthResponse> {
  const settings = loadSettings()
  const response = mode === 'login'
    ? await login(request, settings)
    : await register(request, settings)

  settings.authToken = response.token
  saveSettings(settings)
  authState.currentUser = {
    username: response.username,
    role: response.role,
  }

  return response
}

export function signOut() {
  const settings = loadSettings()
  settings.authToken = ''
  saveSettings(settings)
  authState.currentUser = null
}

export function isAdminUser() {
  return authState.currentUser?.role === 'ADMIN'
}
