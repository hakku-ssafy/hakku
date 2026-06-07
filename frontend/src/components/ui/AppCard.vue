<template>
  <component :is="to ? 'router-link' : tag" :to="to" :class="classes">
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    tag?: string
    to?: string
    interactive?: boolean
    padded?: boolean
    soft?: boolean
  }>(),
  { tag: 'div', interactive: false, padded: true, soft: false },
)

const classes = computed(() => [
  'block rounded-2xl border border-line shadow-sm',
  props.soft ? 'bg-surface-soft' : 'bg-surface',
  props.padded ? 'p-5 sm:p-6' : '',
  props.interactive ? 'u-pop hover:border-accent-line' : '',
])
</script>
