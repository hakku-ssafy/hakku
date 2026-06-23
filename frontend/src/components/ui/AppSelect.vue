<template>
  <label class="block">
    <span v-if="label" class="block text-[13px] font-semibold text-ink-soft mb-1.5">{{ label }}</span>
    <div class="relative">
      <select
        v-model="model"
        :disabled="disabled"
        class="appearance-none w-full h-[50px] pl-4 pr-10 text-[14px] bg-surface text-ink rounded-md border border-line-control transition-colors hover:border-line-strong focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
      >
        <slot />
      </select>
      <svg
        class="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-faint"
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
