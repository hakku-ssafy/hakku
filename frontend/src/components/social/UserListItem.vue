<template>
  <router-link
    :to="`/users/${user.id}`"
    class="group flex items-center gap-3 rounded-lg border border-line bg-surface p-3 u-pop hover:border-line-control"
  >
    <div
      class="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-cream text-base font-bold text-ink/70 select-none"
      aria-hidden="true"
    >
      {{ initial }}
    </div>
    <div class="min-w-0">
      <p class="truncate text-sm font-medium text-ink">{{ user.nickname }}</p>
      <p v-if="colorLabel" class="truncate text-sm text-ink-muted">{{ colorLabel }}</p>
    </div>
    <svg
      class="ml-auto h-4 w-4 shrink-0 text-ink-faint transition-all duration-200 ease-out group-hover:translate-x-0.5 group-hover:text-ink"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
    </svg>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserSummary } from '@/types'
import { formatPersonalColor } from '@/types'

const props = defineProps<{ user: UserSummary }>()

const initial = computed(() => props.user.nickname?.charAt(0).toUpperCase() ?? '?')
const colorLabel = computed(() => formatPersonalColor(props.user.personalColor))
</script>
