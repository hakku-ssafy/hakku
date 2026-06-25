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
      class="w-full h-[50px] px-4 bg-surface text-ink rounded-md border border-line-control placeholder:text-ink-faint transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 disabled:opacity-50"
    />
    <span v-if="hint" class="block text-xs text-ink-muted mt-1.5">{{ hint }}</span>
  </label>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { hangulToQwerty } from '@/lib/hangulToQwerty'

const model = defineModel<string | number>()
const props = withDefaults(
  defineProps<{
    label?: string
    type?: string
    placeholder?: string
    required?: boolean
    disabled?: boolean
    hint?: string
    autocomplete?: string
    inputmode?: 'none' | 'text' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal' | 'search'
    /** 한글 입력을 두벌식 영문 키로 변환해 영문만 남긴다 (비밀번호 등). */
    latinOnly?: boolean
  }>(),
  { type: 'text', required: false, disabled: false, latinOnly: false },
)

// IME 한글 입력은 Vue v-model이 조합 완료(compositionend) 후 model을 갱신하므로,
// 그 시점에 영문으로 되돌린다. 영문 입력에 대해서는 멱등이라 추가 변경이 없다.
watch(model, (value) => {
  if (!props.latinOnly || typeof value !== 'string') return
  const converted = hangulToQwerty(value)
  if (converted !== value) model.value = converted
})
</script>
