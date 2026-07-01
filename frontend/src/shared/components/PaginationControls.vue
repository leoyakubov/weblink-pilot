<script setup lang="ts">
import Button from 'primevue/button';
import './PaginationControls.css';

defineProps<{
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (event: 'previous'): void;
  (event: 'next'): void;
}>();
</script>

<template>
  <div class="pagination-controls" role="navigation" aria-label="Pagination">
    <div class="pagination-controls__summary">
      <strong>Page {{ totalPages ? page + 1 : 0 }} of {{ totalPages }}</strong>
      <span>{{ totalElements }} total items, {{ size }} per page</span>
    </div>

    <div class="pagination-controls__actions">
      <Button
        type="button"
        label="Previous"
        icon="pi pi-angle-left"
        severity="secondary"
        variant="outlined"
        :disabled="first || loading"
        @click="emit('previous')"
      />
      <Button
        type="button"
        label="Next"
        icon="pi pi-angle-right"
        icon-pos="right"
        severity="secondary"
        variant="outlined"
        :disabled="last || loading"
        @click="emit('next')"
      />
    </div>
  </div>
</template>
