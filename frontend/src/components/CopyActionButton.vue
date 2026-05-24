<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { copyText } from '@/lib/clipboard'

const props = withDefaults(defineProps<{
  value: string
  label: string
  copiedLabel?: string
  variant?: 'primary' | 'secondary'
}>(), {
  copiedLabel: 'Copied!',
  variant: 'secondary',
})

const isCopied = ref(false)
let resetTimer: ReturnType<typeof setTimeout> | null = null

const buttonClass = computed(() => [
  'button',
  props.variant === 'primary' ? 'button-primary' : 'button-secondary',
  'copy-action',
  { 'copy-action--copied': isCopied.value },
])

async function copyValue() {
  await copyText(props.value)
  isCopied.value = true

  if (resetTimer) {
    clearTimeout(resetTimer)
  }

  resetTimer = setTimeout(() => {
    isCopied.value = false
    resetTimer = null
  }, 1400)
}

watch(
  () => props.value,
  () => {
    isCopied.value = false
  },
)

onBeforeUnmount(() => {
  if (resetTimer) {
    clearTimeout(resetTimer)
  }
})
</script>

<template>
  <button
    :class="buttonClass"
    type="button"
    :aria-label="isCopied ? props.copiedLabel : props.label"
    :data-state="isCopied ? 'copied' : 'idle'"
    :title="isCopied ? props.copiedLabel : props.label"
    @click="copyValue"
  >
    <span class="copy-action__label">{{ props.label }}</span>
    <span v-if="isCopied" class="copy-action__toast" aria-live="polite">{{ props.copiedLabel }}</span>
  </button>
</template>
