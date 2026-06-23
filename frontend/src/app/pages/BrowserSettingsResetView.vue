<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { clearSettings } from '@/shared/services/settings';

const router = useRouter();

onMounted(async () => {
  clearSettings();
  await router.replace({ name: 'home' });
});
</script>

<template>
  <section class="page-grid">
    <article class="card">
      <div class="card-inner stack">
        <p class="eyebrow">Reset</p>
        <h3 class="panel-title">Browser settings cleared.</h3>
        <p class="hero-note">
          The saved API base URL and session hints were removed from this browser. Redirecting you
          back to the app now.
        </p>
      </div>
    </article>
  </section>
</template>
