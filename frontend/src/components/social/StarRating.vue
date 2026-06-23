<template>
  <div
    class="inline-flex items-center gap-0.5"
    :role="editable ? 'radiogroup' : 'img'"
    :aria-label="editable ? '별점 선택' : `5점 만점에 ${rating}점`"
  >
    <button
      v-for="star in 5"
      :key="star"
      type="button"
      :disabled="!editable"
      class="leading-none select-none"
      :class="[
        sizeClass,
        star <= rating ? 'text-accent' : 'text-line-control',
        editable
          ? 'cursor-pointer transition-transform duration-150 ease-out hover:scale-125 focus-visible:scale-125'
          : 'cursor-default',
      ]"
      :aria-label="`${star}점`"
      @click="onPick(star)"
    >
      <span aria-hidden="true">★</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Size = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{ rating: number; editable?: boolean; size?: Size }>(),
  { editable: false, size: 'md' },
)

const emit = defineEmits<{ 'update:rating': [value: number] }>()

const sizeClass = computed(
  () => ({ sm: 'text-sm', md: 'text-lg', lg: 'text-2xl' })[props.size],
)

function onPick(star: number) {
  if (props.editable) emit('update:rating', star)
}
</script>
