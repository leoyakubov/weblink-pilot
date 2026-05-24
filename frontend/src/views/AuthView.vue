<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authenticate, authState } from '@/lib/auth'
import type { AuthCredentialsRequest } from '@/types'

const props = defineProps<{
  mode: 'login' | 'register'
}>()

const router = useRouter()
const form = reactive<AuthCredentialsRequest>({
  username: '',
  password: '',
})
const busy = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const title = computed(() => (props.mode === 'login' ? 'Sign in' : 'Sign up'))
const submitLabel = computed(() => (props.mode === 'login' ? 'Sign in' : 'Create account'))
const switchLabel = computed(() => (props.mode === 'login'
  ? 'Need an account? Sign up'
  : 'Already have an account? Sign in'))
const switchPath = computed(() => (props.mode === 'login' ? '/auth/signup' : '/auth/signin'))

async function submit() {
  busy.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authenticate(props.mode, form)
    successMessage.value = props.mode === 'login'
      ? `Signed in as ${authState.currentUser?.username}`
      : `Created ${authState.currentUser?.username} and signed in`
    await router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Authentication failed'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="page-grid two-col">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
        </div>

        <p class="hero-note">
          Use an account to keep links owned, track private history, and unlock admin monitoring if your role is ADMIN.
          Guest links still work from the home page.
        </p>

        <div class="grid-2">
          <div class="metric">
            <span class="value">Guest</span>
            <span class="label">Quick link creation without an account</span>
          </div>
          <div class="metric">
            <span class="value">JWT</span>
            <span class="label">Signed-in ownership and role checks</span>
          </div>
        </div>

        <div class="list-item">
          <strong>Current mode</strong>
          <p>{{ props.mode === 'login' ? 'Sign in' : 'Sign up' }}</p>
        </div>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div class="section-row">
          <div>
            <p class="eyebrow">{{ title }}</p>
            <h3 class="panel-title">Continue to your workspace</h3>
          </div>
        </div>

        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Username</span>
            <input v-model="form.username" class="input" type="text" placeholder="user" autocomplete="username" />
          </label>
          <label class="form-field">
            <span class="field-label">Password</span>
            <input v-model="form.password" class="input" type="password" placeholder="user123" autocomplete="current-password" />
          </label>

          <div class="actions">
            <button class="button button-primary" type="submit" :disabled="busy">
              {{ busy ? 'Working...' : submitLabel }}
            </button>
          </div>

          <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
            <span class="status-dot"></span>
            {{ errorMessage }}
          </p>
          <p v-else-if="successMessage" class="status" role="status" aria-live="polite">
            <span class="status-dot"></span>
            {{ successMessage }}
          </p>
          <p v-else class="help-text">
            The demo users are pre-seeded in local and dev: <strong>admin / admin123</strong> and <strong>user / user123</strong>.
            Deployed demo instances can seed the same accounts when the bootstrap env vars are set.
          </p>

          <div class="auth-switch">
            <span class="footnote">
              {{ props.mode === 'login' ? 'Need an account?' : 'Already have an account?' }}
            </span>
            <RouterLink class="button button-secondary button-small auth-link-button" :to="switchPath">
              {{ props.mode === 'login' ? 'Sign up' : 'Sign in' }}
            </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>
</template>
