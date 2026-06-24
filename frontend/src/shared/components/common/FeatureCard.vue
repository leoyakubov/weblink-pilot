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
    <h3>{{ props.title }}</h3>
    <p>{{ props.description }}</p>
  </article>
</template>
