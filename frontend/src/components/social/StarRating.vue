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
      :class="[sizeClass, editable ? 'cursor-pointer transition-transform hover:scale-125' : 'cursor-default']"
      :aria-label="`${star}점`"
      @click="onPick(star)"
    >
      <span :style="{ color: star <= rating ? '#ffb020' : 'var(--color-line-strong)' }">★</span>
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
