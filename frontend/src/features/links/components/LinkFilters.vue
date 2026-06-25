<script setup lang="ts">
import Button from 'primevue/button';
import type { LinkCreatorOptionResponse } from '@/shared/types/api';

defineProps<{
  ownerScope: string;
  creator: string;
  creatorOptions: LinkCreatorOptionResponse[];
  loading?: boolean;
  applyLabel?: string;
}>();

const emit = defineEmits<{
  (event: 'update:ownerScope', value: string): void;
  (event: 'update:creator', value: string): void;
  (event: 'apply'): void;
}>();
</script>

<template>
  <form
    class="form-grid analytics-filter-grid embedded-filter-grid"
    @submit.prevent="emit('apply')"
  >
    <label class="form-field">
      <span class="field-label">Owner group</span>
      <select
        :value="ownerScope"
        class="input"
        @change="emit('update:ownerScope', ($event.target as HTMLSelectElement).value)"
      >
        <option value="all">All links</option>
        <option value="admins">Admins</option>
        <option value="users">Users</option>
        <option value="anonymous">Anonymous</option>
      </select>
    </label>

    <label class="form-field">
      <span class="field-label">Creator</span>
      <select
        :value="creator"
        class="input"
        @change="emit('update:creator', ($event.target as HTMLSelectElement).value)"
      >
        <option value="">All</option>
        <option
          v-for="creatorOption in creatorOptions"
          :key="`${creatorOption.role}-${creatorOption.username}`"
          :value="creatorOption.username"
        >
          {{ creatorOption.username }}
        </option>
      </select>
    </label>

    <div class="actions analytics-filter-actions">
      <Button
        type="button"
        class="refresh-button"
        :label="applyLabel ?? 'Apply filters'"
        icon="pi pi-filter"
        severity="secondary"
        variant="outlined"
        :disabled="loading"
        @click="emit('apply')"
      />
    </div>
  </form>
</template>
