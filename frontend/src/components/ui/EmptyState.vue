<template>
  <div class="text-center py-16 sm:py-20">
    <div
      v-if="icon"
      class="mx-auto mb-5 grid place-items-center w-16 h-16 rounded-full bg-accent-soft text-accent text-2xl"
      aria-hidden="true"
    >
      {{ icon }}
    </div>
    <h3 class="u-serif text-lg text-ink">{{ title }}</h3>
    <p v-if="description" class="text-sm text-ink-muted mt-2 max-w-sm mx-auto leading-relaxed">
      {{ description }}
    </p>
    <div v-if="$slots.default" class="mt-7 flex justify-center">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string; icon?: string }>()
</script>
