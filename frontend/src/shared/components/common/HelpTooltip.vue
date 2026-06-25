<script setup lang="ts">
/* global HTMLElement, MouseEvent */
import { onBeforeUnmount, onMounted, ref } from 'vue';

defineProps<{
  buttonLabel: string;
}>();

const open = ref(false);
const root = ref<HTMLElement | null>(null);

function toggle() {
  open.value = !open.value;
}

function closeOnOutsideClick(event: MouseEvent) {
  if (event.target instanceof window.Node && root.value?.contains(event.target)) {
    return;
  }

  open.value = false;
}

onMounted(() => {
  document.addEventListener('click', closeOnOutsideClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', closeOnOutsideClick);
});
</script>

<template>
  <span ref="root" class="help-popover">
    <button
      type="button"
      class="field-help-button"
      :aria-label="buttonLabel"
      :aria-expanded="open"
      @click.stop="toggle"
    >
      ?
    </button>
    <span v-if="open" class="help-text help-text--tooltip" role="tooltip">
      <slot />
    </span>
  </span>
</template>
