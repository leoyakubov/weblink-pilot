<script setup lang="ts">
import CopyActionButton from '@/shared/components/CopyActionButton.vue';

defineProps<{
  value: string;
  label: string;
  copiedLabel?: string;
  variant?: 'primary' | 'secondary';
}>();
</script>

<template>
  <CopyActionButton
    :value="value"
    :label="label"
    :copied-label="copiedLabel"
    :variant="variant"
  />
</template>
