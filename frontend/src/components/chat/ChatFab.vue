<template>
  <div v-if="!isHidden">
    <!-- 최초 방문 시 1회 노출되는 안내 말풍선 -->
    <Transition name="hint">
      <div v-if="!isOpen && showHint" class="chat-hint" role="status">
        <p class="chat-hint__text">학생증 꾸미기 고민?<br><strong>학꾸 AI</strong>에게 물어보세요!</p>
        <button class="chat-hint__close" aria-label="안내 닫기" @click="dismissHint">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </Transition>

    <Transition name="fab">
      <button
        v-if="!isOpen"
        class="chat-fab"
        aria-label="학꾸 AI 도우미 열기"
        @click="openChat"
      >
        <span class="chat-fab__pulse" aria-hidden="true" />
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.184-4.183a1.14 1.14 0 0 1 .778-.332 48.294 48.294 0 0 0 5.83-.498c1.585-.233 2.708-1.626 2.708-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
        </svg>
        <span class="chat-fab__label">AI 도우미</span>
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

const HINT_DISMISSED_KEY = 'hakku:chatFabHintDismissed'

const route = useRoute()
const isOpen = ref(false)
const showHint = ref(localStorage.getItem(HINT_DISMISSED_KEY) !== '1')

const isHidden = computed(() => route.meta.hideChatFab === true)

function dismissHint() {
  showHint.value = false
  localStorage.setItem(HINT_DISMISSED_KEY, '1')
}

function openChat() {
  dismissHint()
  isOpen.value = true
}
</script>

<style scoped>
.chat-fab {
  position: fixed;
  bottom: 4.75rem;
  right: 1rem;
  height: 3.25rem;
  padding: 0 1.125rem 0 0.875rem;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-2));
  color: white;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  box-shadow: 0 4px 16px -2px rgb(255 77 141 / 0.45);
  z-index: 40;
  transition: transform 0.2s var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1)),
              box-shadow 0.2s ease;
}
.chat-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 24px -2px rgb(255 77 141 / 0.55);
}
.chat-fab:active { transform: scale(0.95); }
.chat-fab svg {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}
.chat-fab__label {
  font-size: 0.8125rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

/* 은은한 펄스 링으로 시선 유도 */
.chat-fab__pulse {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: var(--color-accent);
  opacity: 0.35;
  animation: fab-pulse 2.4s ease-out infinite;
  pointer-events: none;
}
@keyframes fab-pulse {
  0% { transform: scale(1); opacity: 0.35; }
  70% { transform: scale(1.25); opacity: 0; }
  100% { transform: scale(1.25); opacity: 0; }
}
@media (prefers-reduced-motion: reduce) {
  .chat-fab__pulse { animation: none; opacity: 0; }
}

/* 안내 말풍선 */
.chat-hint {
  position: fixed;
  bottom: 8.5rem;
  right: 1rem;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  max-width: 230px;
  padding: 0.75rem 0.875rem;
  background: var(--color-canvas);
  border: 1px solid var(--color-line-strong);
  border-radius: var(--radius-xl);
  border-bottom-right-radius: var(--radius-sm);
  box-shadow: 0 8px 24px -4px rgb(255 77 141 / 0.2), 0 2px 8px -2px rgb(0 0 0 / 0.08);
  z-index: 40;
}
.chat-hint__text {
  font-size: 0.8125rem;
  color: var(--color-ink);
  line-height: 1.5;
}
.chat-hint__text strong {
  color: var(--color-accent);
  font-weight: 700;
}
.chat-hint__close {
  flex-shrink: 0;
  width: 1.125rem;
  height: 1.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  border-radius: 50%;
  transition: color 0.15s;
}
.chat-hint__close:hover { color: var(--color-ink); }
.chat-hint__close svg {
  width: 0.75rem;
  height: 0.75rem;
}

@media (min-width: 768px) {
  .chat-fab {
    bottom: 1.5rem;
    right: 1.5rem;
  }
  .chat-hint {
    bottom: 5.5rem;
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

/* Hint transition */
.hint-enter-active,
.hint-leave-active {
  transition: transform 0.25s ease, opacity 0.2s ease;
}
.hint-enter-from,
.hint-leave-to {
  transform: translateY(8px);
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
