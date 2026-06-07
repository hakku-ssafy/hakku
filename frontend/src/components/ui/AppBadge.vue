<template>
  <span :class="classes"><slot /></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'default' | 'accent' | 'outline' | 'solid'
const props = withDefaults(defineProps<{ variant?: Variant }>(), {
  variant: 'default',
})

const map: Record<Variant, string> = {
  default: 'bg-surface-sunken text-ink-soft',
  accent: 'bg-accent-soft text-accent ring-1 ring-accent-line/60',
  outline: 'border border-line-strong text-ink-soft',
  solid: 'u-gradient-accent text-accent-ink',
}

const classes = computed(() => [
  'inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide',
  map[props.variant],
])
</script>
