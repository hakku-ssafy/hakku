<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="fixed inset-0 z-[60] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        :aria-label="title || undefined"
      >
        <div class="modal__scrim absolute inset-0 bg-ink/50 backdrop-blur-sm" @click="close" />
        <div class="modal__panel relative w-full bg-surface rounded-2xl shadow-lg overflow-hidden" :class="widthClass">
          <header
            v-if="title || $slots.header"
            class="flex items-center justify-between gap-4 px-5 py-4 border-b border-line"
          >
            <slot name="header">
              <h3 class="u-serif text-lg text-ink">{{ title }}</h3>
            </slot>
            <button
              type="button"
              class="grid place-items-center w-8 h-8 -mr-1 rounded-full text-ink-muted hover:text-accent hover:bg-accent-soft transition-colors"
              aria-label="닫기"
              @click="close"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" d="M6 6l12 12M18 6 6 18" />
              </svg>
            </button>
          </header>
          <div :class="bodyPadded ? 'p-5' : ''">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'

const open = defineModel<boolean>('open', { required: true })
const props = withDefaults(
  defineProps<{ title?: string; maxWidth?: 'sm' | 'md' | 'lg'; bodyPadded?: boolean }>(),
  { maxWidth: 'md', bodyPadded: true },
)

const widthClass = computed(
  () => ({ sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl' })[props.maxWidth],
)

function close() {
  open.value = false
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) close()
}

watch(open, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
})

onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .modal__panel,
.modal-leave-active .modal__panel {
  transition: transform 0.28s var(--ease-out-expo), opacity 0.28s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal__panel,
.modal-leave-to .modal__panel {
  transform: translateY(12px) scale(0.98);
  opacity: 0;
}
</style>
