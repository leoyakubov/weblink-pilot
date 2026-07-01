<script setup lang="ts">
import './PageIntro.css';
defineProps<{
  eyebrow: string;
  title: string;
  description: string;
}>();
</script>

<template>
  <section class="page-intro">
    <div class="page-intro__header">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1 class="page-intro__title">{{ title }}</h1>
      </div>
      <slot name="actions" />
    </div>
    <p class="page-intro__description">{{ description }}</p>
  </section>
</template>
