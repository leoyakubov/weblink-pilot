<script setup lang="ts">
import Button from 'primevue/button';

defineProps<{
  visible: boolean;
  title: string;
  url: string;
}>();

const emit = defineEmits<{
  close: [];
}>();
</script>

<template>
  <teleport to="body">
    <Transition name="session-notice">
      <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
        <div class="modal-card modal-card--qr card">
          <div class="card-inner stack">
            <div class="section-row">
              <div>
                <p class="eyebrow">QR code</p>
                <h3 class="panel-title">{{ title }}</h3>
              </div>
              <Button
                type="button"
                icon="pi pi-times"
                class="modal-close-button modal-close-button--icon"
                severity="secondary"
                aria-label="Close QR code"
                @click="emit('close')"
              />
            </div>

            <img
              class="qr-image qr-image--compact modal-qr"
              :src="url"
              :alt="`QR code for ${title}`"
            />
          </div>
        </div>
      </div>
    </Transition>
  </teleport>
</template>
