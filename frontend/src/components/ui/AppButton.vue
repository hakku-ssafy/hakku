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

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
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
  'app-btn inline-flex items-center justify-center gap-2 rounded-full font-semibold whitespace-nowrap select-none u-pop disabled:opacity-40 disabled:pointer-events-none disabled:shadow-none'

const sizes: Record<Size, string> = {
  sm: 'h-9 px-4 text-[0.8125rem]',
  md: 'h-11 px-6 text-sm',
  lg: 'h-13 px-8 text-base',
}

const variants: Record<Variant, string> = {
  primary: 'u-gradient-accent text-accent-ink shadow-sm',
  secondary:
    'bg-surface text-ink border border-line-strong hover:border-accent hover:text-accent',
  ghost: 'bg-transparent text-ink-soft hover:bg-accent-soft hover:text-accent',
  danger: 'bg-transparent text-red-600 border border-red-200 hover:bg-red-50',
}

const classes = computed(() => [
  base,
  sizes[props.size],
  variants[props.variant],
  props.block ? 'w-full' : '',
])
</script>

<style scoped>
.h-13 {
  height: 3.25rem;
}
/* 색 전환은 즉시, 리프트(u-pop)는 부드럽게 — transition 충돌 방지 위해 색만 별도 지정 */
.app-btn {
  transition:
    transform 0.28s var(--ease-out-expo),
    box-shadow 0.28s var(--ease-out-expo),
    color 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease;
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
