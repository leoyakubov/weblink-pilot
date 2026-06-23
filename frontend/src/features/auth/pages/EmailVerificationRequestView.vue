<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ApiRequestError } from '@/shared/services/http';
import { requestEmailVerification } from '@/features/auth/repositories/auth.repository';
import AuthNoticeModal from '@/features/auth/components/AuthNoticeModal.vue';

const form = reactive({
  email: '',
});
const busy = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const noticeVisible = ref(false);
const noticeTitle = ref('');
const noticeMessage = ref('');
const noticeActionLabel = ref('');
const noticeActionHandler = ref<null | (() => unknown)>(null);

const title = computed(() => 'Verify email');

function validateForm(): string {
  if (!form.email.trim()) {
    return 'Email is required.';
  }

  return '';
}

function formatError(error: unknown): string {
  if (error instanceof ApiRequestError && error.status === 400) {
    return 'Email is required.';
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Unable to send verification email';
}

function openNotice(
  title: string,
  message: string,
  actionLabel?: string,
  actionHandler?: () => unknown,
) {
  noticeTitle.value = title;
  noticeMessage.value = message;
  noticeActionLabel.value = actionLabel ?? '';
  noticeActionHandler.value = actionHandler ?? null;
  noticeVisible.value = true;
}

function closeNotice() {
  noticeVisible.value = false;
}

function handleNoticeAction() {
  const action = noticeActionHandler.value;
  closeNotice();
  if (action) {
    void action();
  }
}

async function submit() {
  busy.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const validationError = validateForm();
    if (validationError) {
      errorMessage.value = validationError;
      return;
    }

    const response = await requestEmailVerification({ email: form.email });
    if (response.previewLink) {
      openNotice(
        'Demo email ready',
        'Open the verification link to continue the demo flow.',
        'Open verification link',
        () => {
          const popup = window.open(response.previewLink ?? '', '_blank', 'noopener');
          popup?.focus();
        },
      );
      return;
    }

    successMessage.value = 'Verification email sent. Please check your inbox.';
  } catch (error) {
    errorMessage.value = formatError(error);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">{{ title }}</h3>
        </div>

        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Email</span>
            <input
              v-model="form.email"
              class="input"
              type="email"
              placeholder="you@example.com"
              autocomplete="email"
            />
          </label>

          <div class="actions">
            <button class="button button-primary" type="submit" :disabled="busy">
              {{ busy ? 'Working...' : 'Send verification email' }}
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

          <div class="auth-switch">
            <span class="footnote">Already verified?</span>
            <RouterLink
              class="button button-secondary button-small auth-link-button"
              to="/auth/signin"
            >
              Sign in
            </RouterLink>
          </div>
        </form>
      </div>
    </article>
  </section>

  <AuthNoticeModal
    :visible="noticeVisible"
    :title="noticeTitle"
    :message="noticeMessage"
    :action-label="noticeActionLabel"
    @close="closeNotice"
    @action="handleNoticeAction"
  />
</template>
