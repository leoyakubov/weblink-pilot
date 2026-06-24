<script setup lang="ts">
import Button from 'primevue/button';

defineProps<{
  loading: boolean;
}>();

defineEmits<{
  refresh: [];
}>();
</script>

<template>
  <Button
    type="button"
    class="refresh-button"
    :label="loading ? 'Refreshing...' : 'Refresh'"
    icon="pi pi-refresh"
    severity="secondary"
    variant="outlined"
    :disabled="loading"
    @click="$emit('refresh')"
  />
</template>
