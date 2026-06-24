<script setup lang="ts">
defineProps<{
  eyebrow: string;
  title: string;
  description: string;
}>();
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">{{ eyebrow }}</p>
    <h1 class="page-intro__title">{{ title }}</h1>
    <p class="page-intro__description">{{ description }}</p>
  </section>
</template>
