<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">상품 등록</h1>

    <div v-if="!isSeller" role="alert" class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
      판매자만 접근할 수 있습니다.
    </div>

    <template v-else>
      <div v-if="successMessage" role="status" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" role="alert" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
        {{ errorMessage }}
      </div>

      <form class="bg-white rounded-xl border border-gray-100 p-6 space-y-5" @submit.prevent="handleSubmit">
        <div>
          <label for="name" class="block text-sm font-medium text-gray-700 mb-1">상품명</label>
          <input id="name" v-model="name" type="text" required class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
        </div>

        <div>
          <label for="description" class="block text-sm font-medium text-gray-700 mb-1">설명</label>
          <textarea id="description" v-model="description" rows="4" required class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none" />
        </div>

        <div>
          <label for="price" class="block text-sm font-medium text-gray-700 mb-1">가격 (원)</label>
          <input id="price" v-model.number="price" type="number" min="0" required class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
        </div>

        <div>
          <label for="category" class="block text-sm font-medium text-gray-700 mb-1">카테고리</label>
          <select id="category" v-model="category" required class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500">
            <option value="" disabled>카테고리 선택</option>
            <option v-for="cat in PRODUCT_CATEGORIES" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">컬러</label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="color in selectableColors"
              :key="color.value"
              type="button"
              class="px-3 py-1.5 rounded-full text-sm font-medium border transition-colors"
              :class="selectedColors.includes(color.value) ? 'border-purple-500 bg-purple-50 text-purple-700' : 'border-gray-200 text-gray-600 hover:border-purple-300'"
              @click="toggleColor(color.value)"
            >{{ color.label }}</button>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">상품 이미지</label>
          <div
            class="border-2 border-dashed rounded-xl p-6 text-center transition-colors"
            :class="isDragging ? 'border-purple-400 bg-purple-50' : 'border-gray-200 hover:border-gray-300'"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <div v-if="!previewUrl">
              <p class="text-sm text-gray-600 mb-2">이미지를 드래그하거나 선택하세요</p>
              <p class="text-xs text-gray-400 mb-3">JPG, PNG, WEBP (최대 10MB)</p>
              <label class="inline-block cursor-pointer px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700">
                파일 선택
                <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
              </label>
            </div>
            <div v-else class="relative inline-block">
              <img :src="previewUrl" alt="미리보기" class="max-h-48 rounded-lg object-contain" />
              <button type="button" class="absolute top-2 right-2 w-7 h-7 bg-white rounded-full shadow text-gray-500 hover:text-gray-800" @click="clearImage">×</button>
            </div>
          </div>
        </div>

        <div>
          <label for="purchaseUrl" class="block text-sm font-medium text-gray-700 mb-1">구매 링크 (선택)</label>
          <input id="purchaseUrl" v-model="purchaseUrl" type="url" placeholder="https://..." class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" />
        </div>

        <button type="submit" :disabled="loading || !canSubmit" class="w-full py-3 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50">
          <span v-if="loading">{{ uploadingImage ? '이미지 업로드 중...' : '등록 중...' }}</span>
          <span v-else>상품 등록</span>
        </button>
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createProduct } from '@/api/products'
import { uploadProductImage } from '@/api/storage'
import { COLOR_OPTIONS, PRODUCT_CATEGORIES } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const name = ref('')
const description = ref('')
const price = ref<number | null>(null)
const category = ref('')
const selectedColors = ref<string[]>([])
const purchaseUrl = ref('')
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const loading = ref(false)
const uploadingImage = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const selectableColors = COLOR_OPTIONS.filter((c) => c.value !== 'ALL')
const isSeller = computed(() => authStore.user?.role === 'SELLER')

const canSubmit = computed(
  () =>
    name.value.trim() !== '' &&
    description.value.trim() !== '' &&
    price.value !== null &&
    price.value >= 0 &&
    category.value !== '' &&
    selectedColors.value.length > 0 &&
    selectedFile.value !== null
)

function toggleColor(value: string) {
  const idx = selectedColors.value.indexOf(value)
  if (idx >= 0) selectedColors.value.splice(idx, 1)
  else selectedColors.value.push(value)
}

function setFile(file: File) {
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '이미지 파일만 업로드할 수 있습니다.'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = '파일 크기는 10MB 이하여야 합니다.'
    return
  }
  errorMessage.value = ''
  selectedFile.value = file
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
}

function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) setFile(file)
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) setFile(file)
}

function clearImage() {
  selectedFile.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  if (fileInput.value) fileInput.value.value = ''
}

async function handleSubmit() {
  if (!canSubmit.value || !selectedFile.value) return
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    uploadingImage.value = true
    const imageUrl = await uploadProductImage(selectedFile.value)
    uploadingImage.value = false
    await createProduct({
      name: name.value.trim(),
      description: description.value.trim(),
      price: price.value!,
      category: category.value,
      colors: selectedColors.value,
      keyColor: selectedColors.value[0],
      subColor: selectedColors.value[1] ?? undefined,
      imageUrl,
      purchaseUrl: purchaseUrl.value.trim() || undefined
    })
    successMessage.value = '상품이 등록되었습니다.'
    name.value = ''
    description.value = ''
    price.value = null
    category.value = ''
    selectedColors.value = []
    purchaseUrl.value = ''
    clearImage()
  } catch {
    errorMessage.value = '상품 등록에 실패했습니다. 이미지 업로드와 정보를 확인해주세요.'
  } finally {
    loading.value = false
    uploadingImage.value = false
  }
}

onMounted(async () => {
  if (!authStore.user) await authStore.fetchMe()
  if (authStore.user?.role !== 'SELLER') router.replace('/')
})
</script>
