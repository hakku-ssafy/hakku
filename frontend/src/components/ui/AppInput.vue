<template>
  <label class="block">
    <span v-if="label" class="block text-sm font-medium text-ink mb-1.5">
      {{ label }}<span v-if="required" class="text-accent"> *</span>
    </span>
    <input
      v-model="model"
      :type="type"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :inputmode="inputmode"
      class="w-full h-11 px-4 bg-surface text-ink rounded-xl border border-line-strong placeholder:text-ink-muted transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/25 disabled:opacity-50"
    />
    <span v-if="hint" class="block text-xs text-ink-muted mt-1.5">{{ hint }}</span>
  </label>
</template>

<script setup lang="ts">
const model = defineModel<string | number>()
withDefaults(
  defineProps<{
    label?: string
    type?: string
    placeholder?: string
    required?: boolean
    disabled?: boolean
    hint?: string
    autocomplete?: string
    inputmode?: 'none' | 'text' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal' | 'search'
  }>(),
  { type: 'text', required: false, disabled: false },
)
</script>
