<template>
  <span :class="classes"><slot /></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 알약 배지/칩 — 선택=먹색 채움, accent=퍼스널컬러 틴트(components.md B2). */
type Variant = 'default' | 'accent' | 'outline' | 'solid' | 'ink'
const props = withDefaults(defineProps<{ variant?: Variant }>(), {
  variant: 'default',
})

const map: Record<Variant, string> = {
  default: 'bg-cream text-ink-soft',
  accent: 'bg-accent-soft text-accent-ink border border-accent',
  outline: 'bg-surface border border-line text-ink-soft',
  solid: 'bg-accent text-white',
  ink: 'bg-ink text-white',
}

const classes = computed(() => [
  'inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide',
  map[props.variant],
])
</script>
