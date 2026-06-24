<template>
  <div class="chat-window">
    <!-- Header -->
    <div class="chat-header">
      <div class="chat-header__info">
        <div class="chat-header__avatar">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09Z" />
          </svg>
        </div>
        <div>
          <p class="chat-header__name">학꾸 도우미</p>
          <p class="chat-header__sub">학생증 꾸미기 AI 도우미</p>
        </div>
      </div>
      <button class="chat-header__close" @click="$emit('close')" aria-label="닫기">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Login required -->
    <div v-if="!isAuthenticated" class="chat-login">
      <div class="chat-login__icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" />
        </svg>
      </div>
      <p class="chat-login__title">로그인이 필요해요</p>
      <p class="chat-login__desc">학꾸 AI 도우미는 로그인한 회원만<br>이용할 수 있어요.</p>
      <button class="chat-login__btn" @click="goLogin">로그인하러 가기</button>
    </div>

    <!-- Messages -->
    <div v-else ref="messagesEl" class="chat-messages">
      <div v-if="messages.length === 0" class="chat-empty">
        <p>학생증 사진을 첨부하거나 질문을 입력하면<br>꾸미기 방법을 추천해드려요 ✨</p>
      </div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="chat-msg"
        :class="msg.role === 'user' ? 'chat-msg--user' : 'chat-msg--assistant'"
      >
        <div v-if="msg.image" class="chat-msg__image-wrap">
          <img :src="msg.image" alt="첨부 이미지" class="chat-msg__image" />
        </div>
        <div class="chat-msg__bubble">
          <!-- AI 답변은 마크다운을 살균(DOMPurify)해 렌더링, 사용자 입력은 평문 그대로 -->
          <div
            v-if="msg.content && msg.role === 'assistant'"
            class="chat-md"
            v-html="renderMarkdown(msg.content)"
          />
          <span v-else-if="msg.content">{{ msg.content }}</span>
          <span v-else-if="msg.role === 'assistant'" class="chat-typing">
            <span /><span /><span />
          </span>
        </div>
      </div>
    </div>

    <!-- Image preview -->
    <div v-if="isAuthenticated && imagePreview" class="chat-preview">
      <img :src="imagePreview" alt="미리보기" class="chat-preview__img" />
      <button class="chat-preview__remove" @click="removeImage" aria-label="이미지 제거">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Input -->
    <form v-if="isAuthenticated" class="chat-input" @submit.prevent="send">
      <label class="chat-input__attach" aria-label="이미지 첨부">
        <input ref="fileInput" type="file" accept="image/*" class="sr-only" @change="onFileChange" />
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
        </svg>
      </label>
      <input
        v-model="inputText"
        type="text"
        class="chat-input__text"
        placeholder="질문을 입력하세요…"
        :disabled="isStreaming"
      />
      <button
        type="submit"
        class="chat-input__send"
        :disabled="(!inputText.trim() && !selectedFile) || isStreaming"
        aria-label="전송"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 12 3.269 3.125A59.769 59.769 0 0 1 21.485 12 59.768 59.768 0 0 1 3.27 20.875L5.999 12Zm0 0h7.5" />
        </svg>
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { renderMarkdown } from '@/lib/markdown'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  image?: string
}

defineEmits<{ close: [] }>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isAuthenticated = computed(() => authStore.isAuthenticated)

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

const messages = ref<Message[]>([])
const inputText = ref('')
const selectedFile = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const isStreaming = ref(false)
const messagesEl = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
let nextId = 0

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  selectedFile.value = file
  imagePreview.value = URL.createObjectURL(file)
}

function removeImage() {
  selectedFile.value = null
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function send() {
  const text = inputText.value.trim()
  if (!text && !selectedFile.value) return
  if (isStreaming.value) return

  const userMsg: Message = {
    id: nextId++,
    role: 'user',
    content: text,
    image: imagePreview.value ?? undefined,
  }
  messages.value.push(userMsg)

  const assistantMsg: Message = { id: nextId++, role: 'assistant', content: '' }
  messages.value.push(assistantMsg)

  inputText.value = ''
  const file = selectedFile.value
  removeImage()
  scrollToBottom()
  isStreaming.value = true

  try {
    const formData = new FormData()
    formData.append('message', text || '이 학생증 어떻게 꾸미면 좋을까요?')
    if (file) formData.append('image', file)

    const token = localStorage.getItem('accessToken')
    const response = await fetch('/chat/stream', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      body: formData,
    })

    if (response.status === 401) {
      assistantMsg.content = '로그인이 만료되었어요. 다시 로그인한 뒤 이용해주세요.'
      return
    }
    if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    // SSE 라인이 네트워크 청크 경계에서 잘릴 수 있으므로 버퍼에 누적 후 파싱
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        if (!line.startsWith('data: ')) continue
        const data = line.slice(6)
        if (data === '[DONE]') continue
        try {
          const parsed = JSON.parse(data)
          if (parsed.error) {
            assistantMsg.content = parsed.error
          } else if (parsed.text) {
            assistantMsg.content += parsed.text
          }
          scrollToBottom()
        } catch {}
      }
    }
  } catch {
    assistantMsg.content = '일시적인 오류가 발생했어요. 다시 시도해주세요.'
  } finally {
    isStreaming.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  position: fixed;
  bottom: 4.75rem;
  right: 1rem;
  width: 372px;
  max-width: calc(100vw - 2rem);
  height: 540px;
  max-height: calc(100dvh - 6.5rem);
  background: var(--hk-surface);
  border: 1px solid var(--hk-border);
  border-radius: var(--hk-radius-window);
  box-shadow: var(--hk-shadow-window);
  overflow: hidden;
  z-index: 60;
  animation: hk-pop 0.32s var(--ease-out-expo) both;
}

@media (min-width: 768px) {
  .chat-window {
    bottom: 1.5rem;
    right: 1.5rem;
  }
}

/* Header */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--hk-border-soft);
  background: var(--hk-surface-warm);
}
.chat-header__info {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}
.chat-header__avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: var(--hk-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent, #fff);
  flex-shrink: 0;
}
.chat-header__avatar svg {
  width: 1.1rem;
  height: 1.1rem;
}
.chat-header__name {
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--hk-ink);
  line-height: 1.2;
}
.chat-header__sub {
  font-size: 0.6875rem;
  color: var(--hk-text-muted-2);
}
.chat-header__close {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--hk-text-quiet);
  transition: background 0.15s, color 0.15s;
}
.chat-header__close:hover {
  background: var(--hk-cream);
  color: var(--hk-ink);
}
.chat-header__close svg {
  width: 1rem;
  height: 1rem;
}

/* Login required */
.chat-login {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  text-align: center;
}
.chat-login__icon {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  background: var(--hk-cream);
  color: var(--accent, #16140f);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.25rem;
}
.chat-login__icon svg {
  width: 1.5rem;
  height: 1.5rem;
}
.chat-login__title {
  font-size: 0.9375rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--hk-ink);
}
.chat-login__desc {
  font-size: 0.8125rem;
  color: var(--hk-text-muted);
  line-height: 1.6;
}
.chat-login__btn {
  margin-top: 0.75rem;
  padding: 0.625rem 1.5rem;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-dark);
  color: var(--hk-on-dark);
  font-size: 0.8125rem;
  font-weight: 600;
  transition: transform 0.15s, opacity 0.15s;
}
.chat-login__btn:hover {
  transform: translateY(-1px);
  opacity: 0.92;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  scroll-behavior: smooth;
  background: var(--hk-surface-warm);
}
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--hk-text-muted-2);
  line-height: 1.6;
}
.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}
.chat-msg--user {
  align-self: flex-end;
  align-items: flex-end;
}
.chat-msg--assistant {
  align-self: flex-start;
  align-items: flex-start;
}
.chat-msg__image-wrap {
  margin-bottom: 0.25rem;
}
.chat-msg__image {
  width: 160px;
  height: 120px;
  object-fit: cover;
  border-radius: var(--hk-radius-md);
}
.chat-msg__bubble {
  padding: 0.625rem 0.875rem;
  border-radius: var(--hk-radius-modal);
  font-size: 0.8125rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg--user .chat-msg__bubble {
  background: var(--hk-dark);
  color: var(--hk-on-dark);
  border-bottom-right-radius: var(--hk-radius-cta);
}
.chat-msg--assistant .chat-msg__bubble {
  background: var(--hk-surface);
  color: var(--hk-ink-3);
  border: 1px solid var(--hk-border-soft);
  border-bottom-left-radius: var(--hk-radius-cta);
}

/* 마크다운 렌더링(AI 답변) — pre-wrap 영향 제거 + 블록 요소 간격/링크 스타일 */
.chat-md {
  white-space: normal;
}
.chat-md :first-child {
  margin-top: 0;
}
.chat-md :last-child {
  margin-bottom: 0;
}
.chat-md p {
  margin: 0 0 0.5rem;
}
.chat-md ul,
.chat-md ol {
  margin: 0.25rem 0 0.5rem;
  padding-left: 1.1rem;
}
.chat-md li {
  margin: 0.15rem 0;
}
.chat-md a {
  color: var(--accent, #16140f);
  text-decoration: underline;
  text-underline-offset: 2px;
  font-weight: 600;
}
.chat-md strong {
  font-weight: 700;
}
.chat-md code {
  background: var(--hk-cream);
  padding: 0.05rem 0.3rem;
  border-radius: 4px;
  font-size: 0.92em;
}
.chat-md pre {
  background: var(--hk-cream);
  padding: 0.6rem;
  border-radius: 6px;
  overflow-x: auto;
}
.chat-md pre code {
  background: none;
  padding: 0;
}
.chat-md h1,
.chat-md h2,
.chat-md h3 {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0.5rem 0 0.3rem;
}

/* Typing indicator */
.chat-typing {
  display: flex;
  gap: 0.25rem;
  align-items: center;
  padding: 0.125rem 0;
}
.chat-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hk-text-quiet);
  animation: bounce 1.2s infinite;
}
.chat-typing span:nth-child(2) { animation-delay: 0.2s; }
.chat-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

/* Image preview */
.chat-preview {
  position: relative;
  margin: 0 1rem 0.5rem;
  width: fit-content;
}
.chat-preview__img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: var(--hk-radius-md);
  border: 1px solid var(--hk-border);
}
.chat-preview__remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  background: var(--hk-ink);
  color: var(--hk-on-dark);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-preview__remove svg {
  width: 10px;
  height: 10px;
}

/* Input */
.chat-input {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--hk-border);
  background: var(--hk-surface-warm);
}
.chat-input__attach {
  flex-shrink: 0;
  color: var(--hk-text-muted-2);
  cursor: pointer;
  transition: color 0.15s;
}
.chat-input__attach:hover { color: var(--accent, #16140f); }
.chat-input__attach svg { width: 1.25rem; height: 1.25rem; }
.chat-input__text {
  flex: 1;
  font-size: 0.8125rem;
  color: var(--hk-ink);
  background: transparent;
  outline: none;
  min-width: 0;
}
.chat-input__text::placeholder { color: var(--hk-text-quiet); }
.chat-input__send {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-dark);
  color: var(--hk-on-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.15s, transform 0.15s;
}
.chat-input__send:hover:not(:disabled) { transform: scale(1.06); }
.chat-input__send:disabled { opacity: 0.4; }
.chat-input__send svg { width: 0.875rem; height: 0.875rem; }
</style>
