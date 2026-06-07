<template>
  <div class="u-container max-w-2xl py-10 sm:py-12">
    <div class="border-b border-line pb-4 mb-7">
      <span class="u-eyebrow">Seller</span>
      <h1 class="u-serif text-title text-ink mt-2.5">상품 등록</h1>
    </div>

    <div v-if="!isSeller" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
      판매자만 접근할 수 있습니다.
    </div>

    <template v-else>
      <div v-if="successMessage" role="status" class="mb-4 px-3.5 py-3 bg-surface-sunken border border-line rounded-lg text-ink text-sm">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" role="alert" class="mb-4 px-3.5 py-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
        {{ errorMessage }}
      </div>

      <form class="rounded-xl border border-line bg-surface p-6 space-y-5" @submit.prevent="handleSubmit">
        <AppInput v-model="name" label="상품명" type="text" required />
        <AppTextarea v-model="description" label="설명" :rows="4" required />

        <label class="block">
          <span class="block text-sm font-medium text-ink mb-1.5">가격 (원)</span>
          <input
            v-model.number="price"
            type="number"
            min="0"
            required
            class="w-full h-11 px-3.5 bg-surface text-ink rounded-lg border border-line-strong placeholder:text-ink-muted transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/15 tabular-nums"
          />
        </label>

        <AppSelect v-model="category" label="카테고리">
          <option value="" disabled>카테고리 선택</option>
          <option v-for="cat in PRODUCT_CATEGORIES" :key="cat" :value="cat">{{ cat }}</option>
        </AppSelect>

        <div>
          <span class="block text-sm font-medium text-ink mb-2">컬러</span>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="color in selectableColors"
              :key="color.value"
              type="button"
              class="px-3 py-1.5 rounded-full text-sm font-medium border transition-colors"
              :class="selectedColors.includes(color.value)
                ? 'border-ink bg-ink text-canvas'
                : 'border-line-strong text-ink-soft hover:border-ink-muted hover:text-ink'"
              @click="toggleColor(color.value)"
            >{{ color.label }}</button>
          </div>
        </div>

        <div>
          <span class="block text-sm font-medium text-ink mb-2">상품 이미지</span>
          <div
            class="border-2 border-dashed rounded-xl p-6 text-center transition-colors"
            :class="isDragging ? 'border-accent bg-accent-soft' : 'border-line-strong hover:border-ink-muted'"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <div v-if="!previewUrl">
              <p class="text-sm text-ink-soft mb-2">이미지를 드래그하거나 선택하세요</p>
              <p class="text-xs text-ink-muted mb-3">JPG, PNG, WEBP (최대 10MB)</p>
              <label class="inline-flex items-center cursor-pointer h-10 px-4 bg-accent text-accent-ink rounded-lg text-sm font-medium hover:opacity-90 transition-opacity">
                파일 선택
                <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
              </label>
            </div>
            <div v-else class="relative inline-block">
              <img :src="previewUrl" alt="미리보기" class="max-h-48 rounded-lg object-contain" />
              <button type="button" aria-label="이미지 제거" class="absolute top-2 right-2 w-7 h-7 bg-canvas rounded-full shadow grid place-items-center text-ink-muted hover:text-ink" @click="clearImage">×</button>
            </div>
          </div>
        </div>

        <AppInput v-model="purchaseUrl" label="구매 링크 (선택)" type="url" placeholder="https://..." />

        <AppButton type="submit" block size="lg" :disabled="loading || !canSubmit" :loading="loading">
          {{ loading ? (uploadingImage ? '이미지 업로드 중...' : '등록 중...') : '상품 등록' }}
        </AppButton>
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
import AppInput from '@/components/ui/AppInput.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AppButton from '@/components/ui/AppButton.vue'

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
