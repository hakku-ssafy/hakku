<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">AI 퍼스널컬러 진단</h1>
      <p class="text-gray-500 mt-1 text-sm">얼굴 사진을 업로드하면 AI가 퍼스널컬러를 분석해드려요</p>
    </div>

    <div v-if="!result">
      <div
        class="bg-white rounded-xl border-2 border-dashed border-gray-200 p-12 text-center transition-colors"
        :class="isDragging ? 'border-purple-400 bg-purple-50' : 'hover:border-gray-300'"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <div v-if="!previewUrl">
          <div class="w-16 h-16 bg-purple-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
          </div>
          <p class="text-gray-700 font-medium mb-1">사진을 드래그하거나 클릭해서 업로드</p>
          <p class="text-gray-400 text-sm">JPG, PNG, WEBP (최대 10MB)</p>
          <label class="mt-4 inline-block cursor-pointer">
            <span class="px-5 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors">
              파일 선택
            </span>
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleFileChange"
            />
          </label>
        </div>

        <div v-else class="relative inline-block">
          <img :src="previewUrl" alt="업로드된 이미지" class="max-h-72 rounded-lg object-contain" />
          <button
            type="button"
            aria-label="이미지 제거"
            class="absolute top-2 right-2 w-7 h-7 bg-white rounded-full shadow flex items-center justify-center text-gray-400 hover:text-gray-700 transition-colors"
            @click="clearFile"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div
        v-if="errorMessage"
        role="alert"
        class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm"
      >
        {{ errorMessage }}
      </div>

      <button
        type="button"
        :disabled="!selectedFile || loading"
        class="mt-5 w-full py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        @click="runDiagnosis"
      >
        <span v-if="loading" class="flex items-center justify-center gap-2">
          <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          분석 중...
        </span>
        <span v-else>진단 시작</span>
      </button>
    </div>

    <div v-else class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="bg-gradient-to-br from-purple-50 to-pink-50 p-8 text-center">
        <img
          v-if="result.imageUrl"
          :src="result.imageUrl"
          alt="진단 결과 이미지"
          class="w-32 h-32 rounded-full object-cover mx-auto mb-4 border-4 border-white shadow"
        />
        <p class="text-sm text-purple-500 font-medium mb-1">당신의 퍼스널컬러</p>
        <h2 class="text-3xl font-bold text-gray-900">{{ result.personalColorType }}</h2>
      </div>

      <div class="p-6">
        <p class="text-gray-500 text-sm text-center mb-6">
          진단 결과를 바탕으로 어울리는 상품을 추천해드려요.
        </p>
        <div class="flex gap-3">
          <router-link
            to="/products"
            class="flex-1 py-2.5 bg-purple-600 text-white text-center rounded-lg font-medium hover:bg-purple-700 transition-colors text-sm"
          >
            추천 상품 보기
          </router-link>
          <button
            type="button"
            class="flex-1 py-2.5 border border-gray-200 text-gray-600 rounded-lg font-medium hover:bg-gray-50 transition-colors text-sm"
            @click="resetDiagnosis"
          >
            다시 진단하기
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

interface DiagnosisResult {
  personalColorType: string
  imageUrl: string | null
}

const aiClient = axios.create({
  baseURL: import.meta.env.VITE_AI_BASE_URL ?? '/ai'
})

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const isDragging = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const result = ref<DiagnosisResult | null>(null)

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

function resetDiagnosis() {
  result.value = null
  clearFile()
}

async function runDiagnosis() {
  if (!selectedFile.value) return
  loading.value = true
  errorMessage.value = ''

  try {
    const token = localStorage.getItem('accessToken')
    const formData = new FormData()
    formData.append('image', selectedFile.value)

    const { data } = await aiClient.post<DiagnosisResult>('/api/diagnosis', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    })

    result.value = data
  } catch (e: unknown) {
    if (axios.isAxiosError(e)) {
      errorMessage.value = (e.response?.data as { error?: string })?.error ?? '진단에 실패했습니다. 다시 시도해주세요.'
    } else {
      errorMessage.value = '진단에 실패했습니다. 다시 시도해주세요.'
    }
  } finally {
    loading.value = false
  }
}
</script>
