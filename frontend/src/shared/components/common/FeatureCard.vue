<script setup lang="ts">
const props = defineProps<{
  tone: 'info' | 'success' | 'warn' | 'contrast' | 'accent' | 'muted';
  eyebrow: string;
  title: string;
  description: string;
}>();
</script>

<template>
  <article class="feature-card" :class="`feature-card--${props.tone}`">
    <p class="feature-card__eyebrow">{{ props.eyebrow }}</p>
    <p class="feature-card__summary">
      <strong>{{ props.title }}:</strong>
      {{ props.description }}
    </p>
  </article>
</template>
