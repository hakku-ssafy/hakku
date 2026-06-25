<template>
  <div class="u-container u-container--mypage py-10 sm:py-12">
    <header class="border-b border-line pb-5 mb-7">
      <span class="u-eyebrow">Seller</span>
      <h1 class="u-serif text-title text-ink mt-2.5">상품 등록</h1>
    </header>

    <div v-if="!authChecked" role="status" class="px-4 py-4 text-ink-muted text-sm">
      권한 확인 중…
    </div>
    <div v-else-if="!canRegister" role="alert" class="px-4 py-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
      상품 등록 권한이 없습니다.
    </div>

    <template v-else>
      <div v-if="successMessage" role="status" class="mb-4 px-4 py-3 bg-accent-soft border border-accent rounded-md text-accent-ink text-sm font-medium">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" role="alert" class="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-md text-red-600 text-sm">
        {{ errorMessage }}
      </div>

      <form class="rounded-lg border border-line bg-surface p-6 sm:p-8 space-y-6" @submit.prevent="handleSubmit">
        <AppInput v-model="name" label="상품명" type="text" required />
        <AppTextarea v-model="description" label="설명" :rows="4" required />

        <label class="block">
          <span class="block text-sm font-medium text-ink mb-1.5">가격 (원)<span class="text-accent"> *</span></span>
          <input
            v-model.number="price"
            type="number"
            min="0"
            required
            inputmode="numeric"
            class="w-full h-[50px] px-4 bg-surface text-ink rounded-md border border-line-control placeholder:text-ink-faint transition-colors focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 tabular-nums u-mono"
          />
        </label>

        <AppSelect v-model="category" label="카테고리">
          <option value="" disabled>카테고리 선택</option>
          <option v-for="cat in PRODUCT_CATEGORIES" :key="cat" :value="cat">{{ cat }}</option>
        </AppSelect>

        <div>
          <span class="block text-[13px] font-semibold text-ink-soft mb-2.5">컬러<span class="text-accent"> *</span></span>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="color in selectableColors"
              :key="color.value"
              type="button"
              class="px-3.5 py-2 rounded-full text-sm font-semibold border-[1.5px] transition-colors"
              :class="selectedColors.includes(color.value)
                ? 'border-ink bg-paper-selected text-ink'
                : 'border-line text-ink-soft hover:border-line-strong hover:text-ink'"
              @click="toggleColor(color.value)"
            >{{ color.label }}</button>
          </div>
        </div>

        <div>
          <span class="block text-[13px] font-semibold text-ink-soft mb-2.5">상품 이미지<span class="text-accent"> *</span></span>
          <div
            class="border border-dashed rounded-lg p-8 text-center transition-colors"
            :class="isDragging ? 'border-accent bg-accent-soft' : 'border-line-dashed hover:border-line-strong'"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <div v-if="!previewUrl">
              <p class="text-sm text-ink-soft mb-1.5">이미지를 드래그하거나 선택하세요</p>
              <p class="text-xs text-ink-faint mb-4">JPG, PNG, WEBP (최대 10MB)</p>
              <label class="inline-flex items-center cursor-pointer h-10 px-5 bg-ink text-white rounded-cta text-sm font-semibold hover:bg-ink/90 transition-colors">
                파일 선택
                <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileChange" />
              </label>
            </div>
            <div v-else class="relative inline-block">
              <img :src="previewUrl" alt="미리보기" class="max-h-48 rounded-img object-contain" />
              <button type="button" aria-label="이미지 제거" class="absolute top-2 right-2 w-7 h-7 bg-surface border border-line rounded-full grid place-items-center text-ink-muted hover:text-ink transition-colors" @click="clearImage">×</button>
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
import { useProductStore } from '@/stores/products'
import { createProduct } from '@/api/products'
import { uploadProductImage } from '@/api/storage'
import { COLOR_OPTIONS, PRODUCT_CATEGORIES } from '@/types'
import AppInput from '@/components/ui/AppInput.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AppButton from '@/components/ui/AppButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const productStore = useProductStore()

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
// 권한 확인(fetchMe)이 끝나기 전에는 폼·권한오류 배너를 모두 숨겨 깜빡임을 막는다.
const authChecked = ref(false)

const selectableColors = COLOR_OPTIONS.filter((c) => c.value !== 'ALL')
// 상품 등록은 판매자(SELLER)와 관리자(ADMIN)만 가능하다(백엔드 ProductService.create 와 동일 규칙).
const canRegister = computed(
  () => authStore.user?.role === 'SELLER' || authStore.user?.role === 'ADMIN',
)

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

  // 1단계: 이미지 업로드. 실패 원인을 등록 실패와 구분해 안내한다.
  let imageUrl: string
  try {
    uploadingImage.value = true
    imageUrl = await uploadProductImage(selectedFile.value)
  } catch {
    errorMessage.value = '이미지 업로드에 실패했습니다. 잠시 후 다시 시도해주세요.'
    loading.value = false
    uploadingImage.value = false
    return
  }
  uploadingImage.value = false

  // 2단계: 상품 등록.
  try {
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
    productStore.invalidateProducts() // 새 상품이 다음 목록 방문에 반영되도록 캐시 무효화
    name.value = ''
    description.value = ''
    price.value = null
    category.value = ''
    selectedColors.value = []
    purchaseUrl.value = ''
    clearImage()
  } catch {
    errorMessage.value = '상품 등록에 실패했습니다. 입력 정보를 확인해주세요.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    if (!authStore.user) await authStore.fetchMe()
  } catch {
    // 프로필 조회 실패(네트워크/토큰 만료 등)는 미인증으로 간주해 홈으로 보낸다.
    router.replace('/')
    return
  } finally {
    authChecked.value = true
  }
  if (!canRegister.value) router.replace('/')
})
</script>
