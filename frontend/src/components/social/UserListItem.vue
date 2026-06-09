<template>
  <router-link
    :to="`/users/${user.id}`"
    class="flex items-center gap-3 rounded-xl border border-line bg-surface p-3 u-pop hover:border-accent-line"
  >
    <div class="w-11 h-11 rounded-full u-gradient-accent text-accent-ink grid place-items-center shrink-0">
      <span class="font-bold">{{ initial }}</span>
    </div>
    <div class="min-w-0">
      <p class="text-sm font-semibold text-ink truncate">{{ user.nickname }}</p>
      <p v-if="colorLabel" class="text-xs text-ink-muted truncate">{{ colorLabel }}</p>
    </div>
    <svg class="w-4 h-4 text-ink-muted ml-auto shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
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
