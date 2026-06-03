<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">AI 퍼스널컬러 진단</h1>
      <p class="text-gray-500 mt-1 text-sm">얼굴 사진을 업로드하면 AI가 퍼스널컬러를 분석해드려요</p>
    </div>

    <!-- 프로필 로딩 중 -->
    <div v-if="profileLoading" class="flex justify-center py-20" role="status">
      <svg class="animate-spin h-8 w-8 text-purple-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
      </svg>
    </div>

    <!-- AI 분석 진행 중 (PENDING) -->
    <div v-else-if="diagnosisStatus === 'PENDING'" class="bg-white rounded-xl border border-gray-100 p-8 text-center">
      <div class="w-20 h-20 bg-purple-50 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-10 h-10 text-purple-400 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
        </svg>
      </div>
      <h2 class="text-xl font-bold text-gray-900 mb-2">AI가 분석 중이에요</h2>
      <p class="text-gray-500 text-sm leading-relaxed">
        분석에는 수 분이 걸릴 수 있어요.<br />
        이 페이지를 떠나도 괜찮아요 — 완료되면 알림으로 알려드릴게요!
      </p>
    </div>

    <!-- 진단 완료 (COMPLETED) -->
    <div v-else-if="diagnosisStatus === 'COMPLETED'" class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="bg-gradient-to-br from-purple-50 to-pink-50 p-8 text-center">
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          alt="진단 결과 이미지"
          class="w-32 h-32 rounded-full object-cover mx-auto mb-4 border-4 border-white shadow"
        />
        <p class="text-sm text-purple-500 font-medium mb-1">당신의 퍼스널컬러</p>
        <h2 class="text-3xl font-bold text-gray-900">{{ personalColor }}</h2>
      </div>
      <div class="p-6">
        <p class="text-gray-500 text-sm text-center mb-6">
          진단 결과를 바탕으로 어울리는 상품을 추천해드려요.
        </p>
        <router-link
          to="/recommendations"
          class="block w-full py-2.5 bg-purple-600 text-white text-center rounded-lg font-medium hover:bg-purple-700 transition-colors text-sm"
        >
          추천 상품 보기
        </router-link>
      </div>
    </div>

    <!-- 진단 접수 완료 (로컬 submitted 상태) -->
    <div v-else-if="submitted" class="bg-white rounded-xl border border-gray-100 p-8 text-center">
      <div class="w-16 h-16 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <h2 class="text-xl font-bold text-gray-900 mb-2">진단 요청이 접수되었어요!</h2>
      <p class="text-gray-500 text-sm leading-relaxed">
        AI가 분석을 시작했어요. 페이지를 이동해도 괜찮아요.<br />
        완료되면 알림으로 알려드릴게요!
      </p>
    </div>

    <!-- 업로드 폼 (NONE) -->
    <div v-else>
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
        :disabled="!selectedFile || submitting"
        class="mt-5 w-full py-3 bg-purple-600 text-white rounded-xl font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        @click="runDiagnosis"
      >
        <span v-if="submitting" class="flex items-center justify-center gap-2">
          <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          요청 중...
        </span>
        <span v-else>진단 시작</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import apiClient from '@/api/client'
import type { DiagnosisStatus, User } from '@/types'

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

const profileLoading = ref(true)
const diagnosisStatus = ref<DiagnosisStatus>('NONE')
const personalColor = ref<string | null>(null)
const resultImageUrl = ref<string | null>(null)

onMounted(async () => {
  try {
    const { data } = await apiClient.get<User>('/users/me')
    diagnosisStatus.value = data.diagnosisStatus
    personalColor.value = data.personalColor
    resultImageUrl.value = data.profileImageUrl
  } catch {
    // 인증 없는 경우 등 — 업로드 폼 표시
  } finally {
    profileLoading.value = false
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

    diagnosisStatus.value = 'PENDING'
    submitted.value = true
    clearFile()
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
