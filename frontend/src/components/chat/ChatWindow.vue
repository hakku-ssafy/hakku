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

    <!-- Messages -->
    <div ref="messagesEl" class="chat-messages">
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
          <span v-if="msg.content">{{ msg.content }}</span>
          <span v-else-if="msg.role === 'assistant'" class="chat-typing">
            <span /><span /><span />
          </span>
        </div>
      </div>
    </div>

    <!-- Image preview -->
    <div v-if="imagePreview" class="chat-preview">
      <img :src="imagePreview" alt="미리보기" class="chat-preview__img" />
      <button class="chat-preview__remove" @click="removeImage" aria-label="이미지 제거">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Input -->
    <form class="chat-input" @submit.prevent="send">
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
import { ref, nextTick } from 'vue'

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  image?: string
}

defineEmits<{ close: [] }>()

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

    const response = await fetch('/chat/stream', { method: 'POST', body: formData })
    if (!response.body) throw new Error('No response body')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) {
        if (!line.startsWith('data: ')) continue
        const data = line.slice(6)
        if (data === '[DONE]') break
        try {
          const parsed = JSON.parse(data)
          assistantMsg.content += parsed.text
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
  width: calc(100vw - 2rem);
  max-width: 400px;
  height: 70dvh;
  max-height: 600px;
  background: var(--color-canvas);
  border: 1px solid var(--color-line-strong);
  border-radius: var(--radius-2xl);
  box-shadow: 0 8px 32px -4px rgb(255 77 141 / 0.18), 0 2px 8px -2px rgb(0 0 0 / 0.08);
  overflow: hidden;
  z-index: 60;
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
  border-bottom: 1px solid var(--color-line);
  background: var(--color-surface);
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
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}
.chat-header__avatar svg {
  width: 1.1rem;
  height: 1.1rem;
}
.chat-header__name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1.2;
}
.chat-header__sub {
  font-size: 0.6875rem;
  color: var(--color-ink-muted);
}
.chat-header__close {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  transition: background 0.15s, color 0.15s;
}
.chat-header__close:hover {
  background: var(--color-surface-soft);
  color: var(--color-ink);
}
.chat-header__close svg {
  width: 1rem;
  height: 1rem;
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
}
.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
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
  border-radius: var(--radius-lg);
}
.chat-msg__bubble {
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-xl);
  font-size: 0.8125rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg--user .chat-msg__bubble {
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-2));
  color: white;
  border-bottom-right-radius: var(--radius-sm);
}
.chat-msg--assistant .chat-msg__bubble {
  background: var(--color-surface-soft);
  color: var(--color-ink);
  border-bottom-left-radius: var(--radius-sm);
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
  background: var(--color-ink-muted);
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
  border-radius: var(--radius);
  border: 1px solid var(--color-line);
}
.chat-preview__remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  background: var(--color-ink);
  color: white;
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
  border-top: 1px solid var(--color-line);
  background: var(--color-surface);
}
.chat-input__attach {
  flex-shrink: 0;
  color: var(--color-ink-muted);
  cursor: pointer;
  transition: color 0.15s;
}
.chat-input__attach:hover { color: var(--color-accent); }
.chat-input__attach svg { width: 1.25rem; height: 1.25rem; }
.chat-input__text {
  flex: 1;
  font-size: 0.8125rem;
  color: var(--color-ink);
  background: transparent;
  outline: none;
  min-width: 0;
}
.chat-input__text::placeholder { color: var(--color-ink-muted); }
.chat-input__send {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-2));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.15s;
}
.chat-input__send:disabled { opacity: 0.4; }
.chat-input__send svg { width: 0.875rem; height: 0.875rem; }
</style>
