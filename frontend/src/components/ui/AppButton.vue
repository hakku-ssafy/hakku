<template>
  <component
    :is="tag"
    :to="to"
    :href="href"
    :type="tag === 'button' ? type : undefined"
    :disabled="tag === 'button' ? disabled || loading : undefined"
    :class="classes"
  >
    <span v-if="loading" class="app-btn__spinner" aria-hidden="true" />
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 버튼 — 색·라운드 조합으로 위계 표현(components.md B1).
 * 라운드: 폼 맥락 CTA(lg) 6px, 인라인(sm/md) 알약 999px. shape 로 강제 가능.
 */
type Variant = 'primary' | 'accent' | 'on-dark' | 'soft' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'
type Shape = 'auto' | 'pill' | 'cta'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    shape?: Shape
    to?: string
    href?: string
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    loading?: boolean
    block?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    shape: 'auto',
    type: 'button',
    disabled: false,
    loading: false,
    block: false,
  },
)

const tag = computed(() => {
  if (props.to) return 'router-link'
  if (props.href) return 'a'
  return 'button'
})

const base =
  'app-btn inline-flex items-center justify-center gap-2 font-semibold whitespace-nowrap select-none disabled:opacity-40 disabled:pointer-events-none'

const sizes: Record<Size, string> = {
  sm: 'h-9 px-4 text-[0.8125rem]',
  md: 'h-11 px-6 text-sm',
  lg: 'h-13 px-8 text-[0.9375rem]',
}

/** 먹색 채움이 기본, accent 류는 진단 후 퍼스널컬러로 물든다. */
const variants: Record<Variant, string> = {
  primary: 'bg-ink text-white hover:bg-ink/90',
  accent: 'bg-accent text-white hover:opacity-90',
  'on-dark': 'bg-white text-ink hover:bg-cream',
  soft: 'bg-accent-soft text-accent-ink border border-accent hover:bg-accent-soft/70',
  secondary: 'bg-surface text-ink border border-line-control hover:border-ink',
  ghost: 'bg-transparent text-ink-soft hover:bg-cream hover:text-ink',
  danger: 'bg-transparent text-red-600 border border-red-200 hover:bg-red-50',
}

const radius = computed(() => {
  if (props.shape === 'pill') return 'rounded-full'
  if (props.shape === 'cta') return 'rounded-cta'
  // auto: 큰 폼 CTA 는 6px, 작은 인라인은 알약
  return props.size === 'lg' ? 'rounded-cta' : 'rounded-full'
})

const classes = computed(() => [
  base,
  sizes[props.size],
  radius.value,
  variants[props.variant],
  props.block ? 'w-full' : '',
])
</script>

<style scoped>
.h-13 {
  height: 3.25rem;
}
.app-btn {
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease,
    opacity 0.18s ease;
}
.app-btn__spinner {
  width: 1rem;
  height: 1rem;
  border-radius: 9999px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  animation: app-btn-spin 0.6s linear infinite;
}
@keyframes app-btn-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
