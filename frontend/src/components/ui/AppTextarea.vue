<template>
  <label class="block">
    <span v-if="label" class="block text-[13px] font-semibold text-ink-soft mb-1.5">
      {{ label }}<span v-if="required" class="text-accent"> *</span>
    </span>
    <textarea
      v-model="model"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :rows="rows"
      class="w-full min-h-[96px] px-4 py-3 text-[14px] leading-[1.6] bg-surface text-ink rounded-md border border-line-control placeholder:text-ink-faint transition-colors resize-y hover:border-line-strong focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 disabled:opacity-50 disabled:cursor-not-allowed"
    />
    <span v-if="hint" class="block text-xs text-ink-faint mt-1.5">{{ hint }}</span>
  </label>
</template>

<script setup lang="ts">
const model = defineModel<string>()
withDefaults(
  defineProps<{
    label?: string
    placeholder?: string
    required?: boolean
    disabled?: boolean
    hint?: string
    rows?: number
  }>(),
  { required: false, disabled: false, rows: 5 },
)
</script>
