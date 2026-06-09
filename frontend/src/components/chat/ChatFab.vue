<template>
  <div v-if="!isHidden">
    <Transition name="fab">
      <button
        v-if="!isOpen"
        class="chat-fab"
        aria-label="학꾸 도우미 열기"
        @click="isOpen = true"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.184-4.183a1.14 1.14 0 0 1 .778-.332 48.294 48.294 0 0 0 5.83-.498c1.585-.233 2.708-1.626 2.708-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
        </svg>
      </button>
    </Transition>

    <Transition name="window">
      <ChatWindow v-if="isOpen" @close="isOpen = false" />
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import ChatWindow from './ChatWindow.vue'

const route = useRoute()
const isOpen = ref(false)

const isHidden = computed(() => route.meta.hideChatFab === true)
</script>

<style scoped>
.chat-fab {
  position: fixed;
  bottom: 4.75rem;
  right: 1rem;
  width: 3.25rem;
  height: 3.25rem;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-2));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px -2px rgb(255 77 141 / 0.45);
  z-index: 40;
  transition: transform 0.2s var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1)),
              box-shadow 0.2s ease;
}
.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px -2px rgb(255 77 141 / 0.55);
}
.chat-fab:active { transform: scale(0.95); }
.chat-fab svg {
  width: 1.5rem;
  height: 1.5rem;
}

@media (min-width: 768px) {
  .chat-fab {
    bottom: 1.5rem;
    right: 1.5rem;
  }
}

/* FAB transition */
.fab-enter-active,
.fab-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.fab-enter-from,
.fab-leave-to {
  transform: scale(0.5);
  opacity: 0;
}

/* Window transition */
.window-enter-active,
.window-leave-active {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease;
}
.window-enter-from,
.window-leave-to {
  transform: translateY(16px) scale(0.97);
  opacity: 0;
}
</style>
