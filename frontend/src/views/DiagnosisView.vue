<template>
  <div class="u-container u-container--reading py-10 sm:py-12">
    <div class="mb-9">
      <span class="u-eyebrow">AI Personal Color</span>
      <h1 class="u-serif text-headline text-ink mt-3">퍼스널컬러 진단</h1>
      <p class="text-ink-soft mt-3">얼굴 사진을 올리면 AI가 당신의 계절 타입을 분석해드려요.</p>
    </div>

    <!-- 프로필 로딩 중 -->
    <div v-if="profileLoading" class="flex justify-center py-20" role="status">
      <span class="hk-spinner" aria-hidden="true" />
    </div>

    <!-- AI 분석 진행 중 (PENDING) — E5 로더 -->
    <AppCard v-else-if="diagnosisStatus === 'PENDING'" class="text-center" padded>
      <span class="hk-spinner hk-spinner--lg mx-auto mb-6" role="status" aria-label="분석 중" />
      <h2 class="u-serif text-xl text-ink mb-2">AI가 분석하고 있어요</h2>
      <p class="text-ink-soft text-sm leading-relaxed">
        분석에는 수 분이 걸릴 수 있어요.<br />
        이 페이지를 떠나도 괜찮아요 — 완료되면 알림으로 알려드릴게요!
      </p>
    </AppCard>

    <!-- 진단 완료 (COMPLETED) — 결과 블록(hk-pop, shadow-result, accent 강조) -->
    <div v-else-if="diagnosisStatus === 'COMPLETED'" class="hk-result rounded-block border border-line overflow-hidden">
      <div class="hk-result__head bg-accent-soft px-8 py-10 text-center">
        <img
          v-if="resultImageSrc"
          :src="resultImageSrc"
          alt="진단 결과 이미지"
          class="relative w-32 h-32 rounded-full object-cover mx-auto mb-5 border-4 border-surface shadow-result"
        />
        <span class="u-eyebrow" style="color: var(--accent-ink)">My Personal Color</span>
      </div>
      <div class="p-6 text-center">
        <h2 class="u-serif text-headline text-accent">{{ formatPersonalColor(personalColor) }}</h2>
        <p class="text-ink-soft text-sm mt-3 mb-5">진단 결과를 바탕으로 어울리는 상품을 추천해드려요.</p>
        <div class="space-y-3">
          <AppButton v-if="resultImageSrc" variant="soft" block @click="showResultImage = true">
            진단 이미지 보기
          </AppButton>
          <AppButton variant="accent" to="/recommendations" block>추천 상품 보기</AppButton>
        </div>
      </div>
    </div>

    <!-- 진단 접수 완료 (로컬 submitted 상태) -->
    <AppCard v-else-if="submitted" class="text-center" padded>
      <div class="w-16 h-16 rounded-full bg-accent-soft grid place-items-center mx-auto mb-4">
        <svg class="w-8 h-8 text-accent-ink" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <h2 class="u-serif text-xl text-ink mb-2">진단 요청이 접수되었어요</h2>
      <p class="text-ink-soft text-sm leading-relaxed">
        AI가 분석을 시작했어요. 페이지를 이동해도 괜찮아요.<br />
        완료되면 알림으로 알려드릴게요!
      </p>
    </AppCard>

    <!-- 업로드 폼 (NONE) — E6 드롭존: 1.5px 점선 보더, 원형 아이콘(accent-soft), 안내 + 시작 버튼 -->
    <div v-else>
      <div
        class="hk-dropzone"
        :class="isDragging ? 'is-dragging' : ''"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <div v-if="!previewUrl">
          <div class="w-16 h-16 rounded-full bg-accent-soft grid place-items-center mx-auto mb-4">
            <svg class="w-8 h-8 text-accent-ink" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <p class="text-ink font-medium mb-1">사진을 드래그하거나 클릭해서 업로드</p>
          <p class="text-ink-muted text-sm">JPG, PNG, WEBP (최대 10MB)</p>
          <label class="mt-5 inline-block cursor-pointer">
            <span class="inline-flex items-center h-11 px-6 bg-ink text-white rounded-full text-sm font-semibold transition-colors hover:bg-ink/90">
              파일 선택
            </span>
            <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
          </label>
        </div>

        <div v-else class="relative inline-block">
          <img :src="previewUrl" alt="업로드된 이미지" class="max-h-72 rounded-md object-contain" />
          <button
            type="button"
            aria-label="이미지 제거"
            class="absolute top-2 right-2 w-8 h-8 bg-surface border border-line rounded-full shadow-sm grid place-items-center text-ink-muted hover:text-ink hover:border-line-control transition-colors"
            @click="clearFile"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div
        v-if="errorMessage"
        role="alert"
        class="mt-4 px-3.5 py-3 bg-accent-soft border border-line rounded-md text-ink text-sm"
      >
        {{ errorMessage }}
      </div>

      <AppButton
        class="mt-5"
        block
        size="lg"
        :disabled="!selectedFile || submitting"
        :loading="submitting"
        @click="runDiagnosis"
      >
        진단 시작
      </AppButton>
    </div>

    <!-- 진단 이미지 모달 -->
    <AppModal v-model:open="showResultImage" title="진단 이미지" max-width="md">
      <img v-if="resultImageSrc" :src="resultImageSrc" alt="진단 이미지" class="w-full rounded-md" />
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { formatPersonalColor, type DiagnosisStatus } from '@/types'
import { useAuthedImage } from '@/composables/useAuthedImage'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppModal from '@/components/ui/AppModal.vue'

const aiClient = axios.create({
  baseURL: import.meta.env.VITE_AI_BASE_URL ?? '/ai'
})

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const isDragging = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const errorMessage = ref('')

const showResultImage = ref(false)

const authStore = useAuthStore()

// 진단 상태/결과는 인증 스토어를 단일 소스로 삼는다 → App.vue 폴링이 PENDING →
// COMPLETED 로 갱신하면 새로고침 없이 이 화면이 결과 카드로 즉시 전환된다.
const profileLoading = computed(() => authStore.isAuthenticated && !authStore.user)
const diagnosisStatus = computed<DiagnosisStatus>(() => authStore.user?.diagnosisStatus ?? 'NONE')
const personalColor = computed(() => authStore.user?.personalColor ?? null)
const resultImageUrl = computed(
  () => authStore.user?.diagnosisImageUrl ?? authStore.user?.profileImageUrl ?? null,
)

// 진단 사진은 인증이 필요한 storage 리소스 → 토큰 붙여 fetch 후 object URL 로 표시
const { objectUrl: resultImageSrc } = useAuthedImage(() => resultImageUrl.value)

onMounted(() => {
  if (authStore.isAuthenticated && !authStore.user) {
    authStore.fetchMe().catch(() => {})
  }
})

function setFile(file: File) {
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '이미지 파일만 업로드 가능합니다.'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = '파일 크기는 10MB 이하여야 합니다.'
    return
  }
  errorMessage.value = ''
  selectedFile.value = file
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file)
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files?.[0]) {
    setFile(input.files[0])
  }
}

function handleDrop(event: DragEvent) {
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    setFile(file)
  }
}

function clearFile() {
  selectedFile.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  errorMessage.value = ''
}

async function runDiagnosis() {
  if (!selectedFile.value) return
  submitting.value = true
  errorMessage.value = ''

  try {
    const token = localStorage.getItem('accessToken')
    const formData = new FormData()
    formData.append('image', selectedFile.value)

    await aiClient.post('/api/diagnosis', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    })

    submitted.value = true
    clearFile()
    // 백엔드가 상태를 PENDING 으로 잠갔으니 즉시 반영하고, 이후 폴링이 완료를 감지한다.
    await authStore.fetchMe().catch(() => {})
  } catch (e: unknown) {
    if (axios.isAxiosError(e)) {
      const status = e.response?.status
      if (status === 409) {
        errorMessage.value = '이미 진단 요청이 접수되었습니다.'
      } else {
        errorMessage.value = (e.response?.data as { detail?: string })?.detail ?? '진단에 실패했습니다. 다시 시도해주세요.'
      }
    } else {
      errorMessage.value = '진단에 실패했습니다. 다시 시도해주세요.'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* E5. 로더 — 원형 보더 + accent border-top-color, hk-spin .8s linear infinite */
.hk-spinner {
  display: inline-block;
  width: 32px;
  height: 32px;
  border-radius: var(--hk-radius-pill);
  border: 3px solid var(--hk-border);
  border-top-color: var(--accent);
  animation: hk-spin 0.8s linear infinite;
}
.hk-spinner--lg {
  display: block;
  width: 52px;
  height: 52px;
  border-width: 4px;
}

/* E6. 업로드 드롭존 — 1.5px dashed 보더, 라운드 10px, 중앙 정렬 */
.hk-dropzone {
  padding: 48px 24px;
  text-align: center;
  border: 1.5px dashed var(--hk-border-dashed);
  border-radius: var(--hk-radius-lg);
  background: var(--hk-surface);
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}
.hk-dropzone:hover {
  border-color: var(--accent);
}
.hk-dropzone.is-dragging {
  border-color: var(--accent);
  border-style: solid;
  background: var(--accent-soft);
}

/* 결과 블록 — 떠 있는 강조 레이어: hk-pop 진입 + shadow-result */
.hk-result {
  animation: hk-pop 0.3s var(--ease-out-soft, ease) both;
  box-shadow: var(--hk-shadow-result);
}
</style>
