<template>
  <div class="space-y-2">
    <div v-for="n in count" :key="n" class="rounded-xl border border-line p-4 flex gap-3">
      <SkeletonBlock height="2.75rem" width="2.75rem" class="shrink-0 !rounded-full" />
      <div class="flex-1 space-y-2 pt-1">
        <SkeletonBlock height="0.875rem" width="60%" />
        <SkeletonBlock height="0.75rem" width="35%" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

withDefaults(defineProps<{ count?: number }>(), { count: 4 })
</script>
