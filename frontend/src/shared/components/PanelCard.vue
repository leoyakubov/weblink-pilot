<script setup lang="ts">
import './PanelCard.css';
defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
}>();
</script>

<template>
  <article class="card">
    <div class="card-inner stack">
      <header v-if="$slots.header || eyebrow || title || description" class="panel-card__header">
        <slot name="header">
          <div>
            <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
            <h2 v-if="title" class="panel-title">{{ title }}</h2>
            <p v-if="description" class="panel-card__description">{{ description }}</p>
          </div>
        </slot>
        <slot name="actions" />
      </header>
      <slot />
    </div>
  </article>
</template>
