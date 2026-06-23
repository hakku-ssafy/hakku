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
        <span class="chat-fab__icon" aria-hidden="true">✦</span>
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
  padding: 0 1.25rem 0 0.5rem;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-dark);
  color: var(--hk-on-dark);
  display: flex;
  align-items: center;
  gap: 0.625rem;
  box-shadow: var(--hk-shadow-window);
  z-index: 40;
  animation: hk-pulse 2.6s var(--hk-ease-slide) infinite;
  transition: transform 0.2s var(--ease-out-expo, cubic-bezier(0.16, 1, 0.3, 1));
}
.chat-fab:hover { transform: scale(1.05); }
.chat-fab:active { transform: scale(0.96); }
.chat-fab:focus-visible {
  outline: 2px solid var(--accent, #16140f);
  outline-offset: 3px;
}
/* ✦ 아이콘 — 원형 영역 안 액센트 글리프 */
.chat-fab__icon {
  flex-shrink: 0;
  width: 2.375rem;
  height: 2.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--hk-dark-4);
  color: var(--accent, #fff);
  font-size: 1.0625rem;
  line-height: 1;
}
.chat-fab__label {
  font-size: 0.8125rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  white-space: nowrap;
}
@media (prefers-reduced-motion: reduce) {
  .chat-fab { animation: none; }
}

/* 안내 말풍선 — 웜 뉴트럴, 보더 우선 */
.chat-hint {
  position: fixed;
  bottom: 8.5rem;
  right: 1rem;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  max-width: 230px;
  padding: 0.75rem 0.875rem;
  background: var(--hk-surface);
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-modal);
  border-bottom-right-radius: var(--hk-radius-cta);
  box-shadow: var(--hk-shadow-card);
  z-index: 40;
}
.chat-hint__text {
  font-size: 0.8125rem;
  color: var(--hk-ink-2);
  line-height: 1.5;
}
.chat-hint__text strong {
  color: var(--accent, #16140f);
  font-weight: 700;
}
.chat-hint__close {
  flex-shrink: 0;
  width: 1.125rem;
  height: 1.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--hk-text-quiet);
  border-radius: 50%;
  transition: color 0.15s;
}
.chat-hint__close:hover { color: var(--hk-ink); }
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
