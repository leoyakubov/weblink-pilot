<script setup lang="ts">
import CopyActionButton from '@/components/CopyActionButton.vue';

withDefaults(
  defineProps<{
    value: string;
    label: string;
    copiedLabel?: string;
    variant?: 'primary' | 'secondary';
  }>(),
  {
    copiedLabel: 'Copied!',
    variant: 'secondary',
  },
);
</script>

<template>
  <CopyActionButton v-bind="$props" />
</template>
