<script setup lang="ts">
import { computed, watch } from 'vue';
import Button from 'primevue/button';
import type { LinkCreatorOptionResponse } from '@/shared/types/api';

const props = withDefaults(
  defineProps<{
    ownerScope: string;
    expirationScope: string;
    creator: string;
    creatorOptions: LinkCreatorOptionResponse[];
    showAdminFilters?: boolean;
    loading?: boolean;
    applyLabel?: string;
  }>(),
  {
    showAdminFilters: false,
    loading: false,
    applyLabel: 'Apply filters',
  },
);

const emit = defineEmits<{
  (event: 'update:ownerScope', value: string): void;
  (event: 'update:expirationScope', value: string): void;
  (event: 'update:creator', value: string): void;
  (event: 'apply'): void;
}>();

const roleByScope: Record<string, string> = {
  admins: 'ADMIN',
  users: 'USER',
  anonymous: 'ANONYMOUS',
};

const filteredCreatorOptions = computed(() => {
  const expectedRole = roleByScope[props.ownerScope];

  if (!expectedRole) {
    return props.creatorOptions;
  }

  return props.creatorOptions.filter(
    (creatorOption) => creatorOption.role.toUpperCase() === expectedRole,
  );
});

watch(
  () => [props.ownerScope, props.creatorOptions, props.creator] as const,
  () => {
    if (!props.creator) {
      return;
    }

    if (!props.showAdminFilters) {
      emit('update:creator', '');
      return;
    }

    const creatorStillAvailable = filteredCreatorOptions.value.some(
      (creatorOption) => creatorOption.username === props.creator,
    );

    if (!creatorStillAvailable) {
      emit('update:creator', '');
    }
  },
  { immediate: true },
);
</script>

<template>
  <form
    class="form-grid analytics-filter-grid embedded-filter-grid"
    @submit.prevent="emit('apply')"
  >
    <label class="form-field">
      <span class="field-label">Expiration</span>
      <select
        :value="expirationScope"
        class="input"
        @change="emit('update:expirationScope', ($event.target as HTMLSelectElement).value)"
      >
        <option value="all">All</option>
        <option value="active">Active</option>
        <option value="expired">Expired</option>
        <option value="never">Never expires</option>
      </select>
    </label>

    <label v-if="showAdminFilters" class="form-field">
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

    <label v-if="showAdminFilters" class="form-field">
      <span class="field-label">Creator</span>
      <select
        :value="creator"
        class="input"
        @change="emit('update:creator', ($event.target as HTMLSelectElement).value)"
      >
        <option value="">All</option>
        <option
          v-for="creatorOption in filteredCreatorOptions"
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
