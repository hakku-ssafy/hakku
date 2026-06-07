<template>
  <label class="block">
    <span v-if="label" class="block text-sm font-medium text-ink mb-1.5">{{ label }}</span>
    <div class="relative">
      <select
        v-model="model"
        :disabled="disabled"
        class="appearance-none w-full h-11 pl-4 pr-10 bg-surface text-ink rounded-xl border border-line-strong transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/25 disabled:opacity-50 cursor-pointer"
      >
        <slot />
      </select>
      <svg
        class="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-muted"
        viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="m5 7.5 5 5 5-5" />
      </svg>
    </div>
  </label>
</template>

<script setup lang="ts">
const model = defineModel<string | number>()
withDefaults(defineProps<{ label?: string; disabled?: boolean }>(), {
  disabled: false,
})
</script>
