<script setup lang="ts">
import AuthFeatureView from '@/features/auth/AuthView.vue';

defineProps<{
  mode: 'login' | 'register';
}>();
</script>

<template>
  <AuthFeatureView :mode="mode" />
</template>
