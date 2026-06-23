<template>
  <div class="space-y-2" role="status" aria-busy="true" aria-label="불러오는 중">
    <div
      v-for="n in count"
      :key="n"
      class="flex gap-3 rounded-lg border border-line-soft bg-surface p-4"
    >
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
