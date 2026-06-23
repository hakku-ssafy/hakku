<template>
  <component :is="to ? 'router-link' : tag" :to="to" :class="classes">
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 서피스 카드 — 흰 면 + 1px 보더 + 10px 라운드, 그림자 없음(components.md C7). */
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
  'block rounded-lg border border-line',
  props.soft ? 'bg-surface-soft' : 'bg-surface',
  props.padded ? 'p-5 sm:p-6' : '',
  props.interactive ? 'u-pop hover:border-line-control' : '',
])
</script>
