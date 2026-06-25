<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import Button from 'primevue/button';

const props = defineProps<{
  loading: boolean;
  label?: string;
}>();

const emit = defineEmits<{
  refresh: [];
}>();

const refreshed = ref(false);
const refreshRequested = ref(false);
let resetTimer: number | undefined;

const idleLabel = computed(() => props.label ?? 'Refresh');
const buttonLabel = computed(() => {
  if (props.loading) {
    return 'Refreshing...';
  }

  return refreshed.value ? 'Refreshed' : idleLabel.value;
});
const buttonIcon = computed(() =>
  refreshed.value && !props.loading ? 'pi pi-check' : 'pi pi-refresh',
);

function showRefreshedState() {
  refreshed.value = true;
  refreshRequested.value = false;

  if (resetTimer) {
    window.clearTimeout(resetTimer);
  }

  resetTimer = window.setTimeout(() => {
    refreshed.value = false;
  }, 1800);
}

function requestRefresh() {
  if (props.loading) {
    return;
  }

  refreshRequested.value = true;
  emit('refresh');

  window.setTimeout(() => {
    if (refreshRequested.value && !props.loading) {
      showRefreshedState();
    }
  }, 0);
}

watch(
  () => props.loading,
  (loading, wasLoading) => {
    if (!loading && wasLoading && refreshRequested.value) {
      showRefreshedState();
    }
  },
);

onBeforeUnmount(() => {
  if (resetTimer) {
    window.clearTimeout(resetTimer);
  }
});
</script>

<template>
  <Button
    type="button"
    class="refresh-button"
    :label="buttonLabel"
    :icon="buttonIcon"
    severity="secondary"
    variant="outlined"
    :disabled="loading"
    @click="requestRefresh"
  />
</template>
