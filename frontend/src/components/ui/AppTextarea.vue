<template>
  <label class="block">
    <span v-if="label" class="block text-sm font-medium text-ink mb-1.5">
      {{ label }}<span v-if="required" class="text-accent"> *</span>
    </span>
    <textarea
      v-model="model"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :rows="rows"
      class="w-full px-4 py-3 bg-surface text-ink rounded-xl border border-line-strong placeholder:text-ink-muted transition-colors resize-y focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/25 disabled:opacity-50"
    />
    <span v-if="hint" class="block text-xs text-ink-muted mt-1.5">{{ hint }}</span>
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
