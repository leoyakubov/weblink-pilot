<script setup lang="ts">
import Button from 'primevue/button';

defineProps<{
  visible: boolean;
  title: string;
  message: string;
  actionLabel?: string;
}>();

const emit = defineEmits<{
  close: [];
  action: [];
}>();
</script>

<template>
  <Transition name="session-notice">
    <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
      <article class="card modal-card auth-notice-modal">
        <div class="card-inner stack">
          <div class="auth-heading">
            <p class="eyebrow">Account notice</p>
            <h3 class="panel-title">{{ title }}</h3>
          </div>

          <p class="hero-note">{{ message }}</p>

          <div class="actions">
            <Button
              v-if="actionLabel"
              type="button"
              :label="actionLabel"
              icon="pi pi-arrow-right"
              @click="emit('action')"
            />
            <Button
              type="button"
              label="Close"
              severity="secondary"
              variant="outlined"
              @click="emit('close')"
            />
          </div>
        </div>
      </article>
    </div>
  </Transition>
</template>
