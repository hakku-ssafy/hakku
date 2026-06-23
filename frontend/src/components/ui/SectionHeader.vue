<template>
  <div class="flex items-end justify-between gap-4 border-b border-line pb-4 mb-7">
    <div class="min-w-0">
      <span v-if="eyebrow" class="u-keeper inline-flex items-center gap-1.5">
        <span class="inline-block w-1.5 h-1.5 rounded-full bg-accent" aria-hidden="true" />
        {{ eyebrow }}
      </span>
      <h2
        class="text-[1.55rem] font-bold tracking-tight text-ink leading-tight"
        :class="eyebrow ? 'mt-2.5' : ''"
      >
        {{ title }}
      </h2>
      <p v-if="description" class="text-sm text-ink-soft mt-2">{{ description }}</p>
    </div>
    <div v-if="$slots.action" class="shrink-0 pb-1 text-[13px] text-ink-soft transition-colors hover:text-ink">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ eyebrow?: string; title: string; description?: string }>()
</script>
