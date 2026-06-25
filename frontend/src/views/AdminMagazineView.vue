<template>
  <div class="u-container admin">
    <header class="admin__head">
      <span class="u-eyebrow">Admin</span>
      <h1 class="admin__title">매거진 관리</h1>
      <p class="admin__desc">
        메인 캐러셀에 노출되는 매거진을 발행·수정·정렬합니다. 본문은 마크다운으로 작성하고,
        <code>/products/123</code> 링크를 한 줄로 넣으면 상품이 가로 카드로 임베드됩니다.
      </p>
    </header>

    <!-- ===== 작성/수정 폼 ===== -->
    <form class="card-form" @submit.prevent="save">
      <div class="card-form__head">
        <h2 class="card-form__title">{{ editingId ? '매거진 수정' : '새 매거진 발행' }}</h2>
        <button v-if="editingId" type="button" class="link-btn" @click="resetForm">새 매거진으로 전환</button>
      </div>

      <div class="field-grid">
        <label class="field">
          <span class="field__label">키커 (작은 라벨)</span>
          <input v-model="form.kicker" type="text" class="input" placeholder="EDITORIAL" maxlength="80" />
        </label>
        <label class="field">
          <span class="field__label">정렬 순서</span>
          <input v-model.number="form.displayOrder" type="number" class="input" min="0" />
        </label>
      </div>

      <label class="field">
        <span class="field__label">제목 <em class="req">*</em></span>
        <input v-model="form.title" type="text" class="input" placeholder="이주의 다꾸 특집" maxlength="200" required />
      </label>

      <label class="field">
        <span class="field__label">문구 (부제)</span>
        <input v-model="form.subtitle" type="text" class="input" placeholder="겨울 감성 데코 모음" maxlength="300" />
      </label>

      <label class="field">
        <span class="field__label">커버 이미지</span>
        <div class="uploader">
          <div class="uploader__preview" :class="{ 'uploader__preview--empty': !form.coverImageUrl }">
            <img v-if="form.coverImageUrl" :src="form.coverImageUrl" alt="미리보기" />
            <span v-else aria-hidden="true">◍</span>
          </div>
          <div class="uploader__actions">
            <label class="btn btn--ghost">
              {{ uploadingCover ? '업로드 중…' : '이미지 선택' }}
              <input type="file" accept="image/*" class="sr-only" :disabled="uploadingCover" @change="onCoverChange" />
            </label>
            <button v-if="form.coverImageUrl" type="button" class="link-btn" @click="form.coverImageUrl = ''">제거</button>
          </div>
        </div>
      </label>

      <label class="field">
        <span class="field__label">본문 (마크다운)</span>
        <textarea
          v-model="form.content"
          class="input textarea"
          rows="12"
          placeholder="## 소제목&#10;&#10;사진과 글을 마크다운으로 작성하세요.&#10;&#10;![설명](이미지URL)&#10;&#10;/products/123"
        ></textarea>
        <span class="field__hint">
          사진은 <code>![설명](URL)</code>, 학꾸 상품은 <code>/products/123</code> 링크를 한 줄로 넣으면 카드로 임베드됩니다.
        </span>
        <div class="content-tools">
          <label class="btn btn--ghost btn--sm">
            {{ uploadingBody ? '업로드 중…' : '＋ 본문에 사진 추가' }}
            <input type="file" accept="image/*" class="sr-only" :disabled="uploadingBody" @change="onBodyImageChange" />
          </label>
        </div>
      </label>

      <label class="toggle">
        <input v-model="form.published" type="checkbox" />
        <span>메인에 노출 (발행)</span>
      </label>

      <p v-if="errorMsg" class="card-form__error">{{ errorMsg }}</p>

      <div class="card-form__actions">
        <button type="submit" class="btn btn--solid" :disabled="!canSave">
          {{ saving ? '저장 중…' : editingId ? '수정 저장' : '발행' }}
        </button>
      </div>
    </form>

    <!-- ===== 매거진 목록 ===== -->
    <section class="list">
      <h2 class="list__title">등록된 매거진 <span class="list__count">{{ magazines.length }}</span></h2>

      <div v-if="loading" role="status" class="list__center">불러오는 중…</div>
      <p v-else-if="magazines.length === 0" class="list__empty">아직 발행된 매거진이 없습니다.</p>

      <ul v-else class="list__items">
        <li v-for="mag in magazines" :key="mag.id" class="row" :class="{ 'row--inactive': !mag.published }">
          <div class="row__thumb" :class="{ 'row__thumb--empty': !mag.coverImageUrl }">
            <img v-if="mag.coverImageUrl" :src="mag.coverImageUrl" :alt="mag.title" />
            <span v-else aria-hidden="true">◍</span>
          </div>
          <div class="row__main">
            <div class="row__top">
              <span class="row__order">#{{ mag.displayOrder }}</span>
              <span v-if="!mag.published" class="row__badge">미발행</span>
              <router-link :to="`/magazine/${mag.id}`" class="row__dest">/magazine/{{ mag.id }}</router-link>
            </div>
            <p class="row__name">{{ mag.title }}</p>
            <p v-if="mag.subtitle" class="row__sub">{{ mag.subtitle }}</p>
          </div>
          <div class="row__actions">
            <button type="button" class="link-btn" @click="startEdit(mag)">수정</button>
            <button type="button" class="link-btn link-btn--danger" @click="remove(mag)">삭제</button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  listAllMagazines,
  createMagazine,
  updateMagazine,
  deleteMagazine,
} from '@/api/magazine'
import { uploadMagazineImage } from '@/api/storage'
import type { Magazine, MagazineInput } from '@/types'

const magazines = ref<Magazine[]>([])
const loading = ref(false)
const saving = ref(false)
const uploadingCover = ref(false)
const uploadingBody = ref(false)
const editingId = ref<number | null>(null)
const errorMsg = ref<string | null>(null)

const form = reactive({
  kicker: '',
  title: '',
  subtitle: '',
  content: '',
  coverImageUrl: '',
  displayOrder: 0,
  published: true,
})

const canSave = computed(
  () => form.title.trim().length > 0 && !saving.value && !uploadingCover.value && !uploadingBody.value,
)

async function load() {
  loading.value = true
  try {
    magazines.value = await listAllMagazines()
  } catch {
    errorMsg.value = '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  errorMsg.value = null
  Object.assign(form, {
    kicker: '',
    title: '',
    subtitle: '',
    content: '',
    coverImageUrl: '',
    displayOrder: magazines.value.length,
    published: true,
  })
}

function startEdit(mag: Magazine) {
  editingId.value = mag.id
  errorMsg.value = null
  Object.assign(form, {
    kicker: mag.kicker ?? '',
    title: mag.title,
    subtitle: mag.subtitle ?? '',
    content: mag.content ?? '',
    coverImageUrl: mag.coverImageUrl ?? '',
    displayOrder: mag.displayOrder,
    published: mag.published,
  })
  if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function onCoverChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingCover.value = true
  errorMsg.value = null
  try {
    form.coverImageUrl = await uploadMagazineImage(file)
  } catch {
    errorMsg.value = '이미지 업로드에 실패했습니다.'
  } finally {
    uploadingCover.value = false
    input.value = ''
  }
}

/** 본문 이미지 업로드 → 마크다운 이미지 문법을 본문 끝에 덧붙인다. */
async function onBodyImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingBody.value = true
  errorMsg.value = null
  try {
    const url = await uploadMagazineImage(file)
    const prefix = form.content.trim().length > 0 ? `${form.content.replace(/\s+$/, '')}\n\n` : ''
    form.content = `${prefix}![](${url})\n`
  } catch {
    errorMsg.value = '이미지 업로드에 실패했습니다.'
  } finally {
    uploadingBody.value = false
    input.value = ''
  }
}

function buildInput(): MagazineInput {
  const nz = (s: string) => {
    const t = s.trim()
    return t.length > 0 ? t : null
  }
  return {
    kicker: nz(form.kicker),
    title: form.title.trim(),
    subtitle: nz(form.subtitle),
    content: nz(form.content),
    coverImageUrl: nz(form.coverImageUrl),
    displayOrder: Number(form.displayOrder) || 0,
    published: form.published,
  }
}

async function save() {
  if (!canSave.value) return
  saving.value = true
  errorMsg.value = null
  try {
    const input = buildInput()
    if (editingId.value != null) {
      await updateMagazine(editingId.value, input)
    } else {
      await createMagazine(input)
    }
    await load()
    resetForm()
  } catch {
    errorMsg.value = '저장에 실패했습니다. 다시 시도해 주세요.'
  } finally {
    saving.value = false
  }
}

async function remove(mag: Magazine) {
  if (typeof window !== 'undefined' && !window.confirm(`'${mag.title}' 매거진을 삭제할까요?`)) return
  try {
    await deleteMagazine(mag.id)
    if (editingId.value === mag.id) resetForm()
    await load()
  } catch {
    errorMsg.value = '삭제에 실패했습니다.'
  }
}

onMounted(async () => {
  await load()
  form.displayOrder = magazines.value.length
})
</script>

<style scoped>
.admin {
  padding-top: 40px;
  padding-bottom: 90px;
}
.admin__head {
  margin-bottom: 28px;
}
.admin__title {
  margin-top: 10px;
  font-size: clamp(1.6rem, 1.3rem + 1.4vw, 2rem);
  font-weight: 800;
  letter-spacing: -0.03em;
}
.admin__desc {
  margin-top: 8px;
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--hk-text-quiet);
}
.admin__desc code,
.field__hint code {
  font-family: var(--font-mono);
  font-size: 0.85em;
  background: var(--hk-cream);
  padding: 1px 5px;
  border-radius: 4px;
  color: var(--hk-ink);
}

/* ── 폼 ── */
.card-form {
  padding: 26px;
  border-radius: var(--hk-radius-md);
  border: 1px solid var(--hk-border);
  background: var(--hk-surface-warm);
  margin-bottom: 44px;
}
.card-form__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.card-form__title {
  font-size: 1.05rem;
  font-weight: 700;
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr 140px;
  gap: 14px;
}
.field {
  display: block;
  margin-bottom: 16px;
}
.field__label {
  display: block;
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hk-ink);
}
.req {
  color: #c0392b;
  font-style: normal;
}
.field__hint {
  display: block;
  margin-top: 6px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--hk-text-quiet);
}
.input {
  width: 100%;
  height: 42px;
  padding: 0 13px;
  border-radius: var(--hk-radius-cta);
  border: 1px solid var(--hk-border);
  background: var(--hk-surface);
  font-size: 14px;
  color: var(--hk-ink);
  transition: border-color 0.18s ease;
}
.input:focus {
  outline: none;
  border-color: var(--accent, #16140f);
}
.textarea {
  height: auto;
  padding: 11px 13px;
  line-height: 1.7;
  resize: vertical;
  font-family: var(--font-mono);
  font-size: 13px;
}
.content-tools {
  margin-top: 10px;
}

/* 이미지 업로더 */
.uploader {
  display: flex;
  align-items: center;
  gap: 16px;
}
.uploader__preview {
  width: 92px;
  height: 92px;
  flex: 0 0 auto;
  border-radius: var(--hk-radius-cta);
  overflow: hidden;
  background: var(--hk-cream);
  display: flex;
  align-items: center;
  justify-content: center;
}
.uploader__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.uploader__preview--empty {
  color: var(--hk-text-quiet);
  font-size: 26px;
}
.uploader__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 토글 */
.toggle {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 8px;
}
.toggle input {
  width: 17px;
  height: 17px;
  accent-color: var(--accent, #16140f);
}

.card-form__error {
  margin: 10px 0 0;
  font-size: 13px;
  color: #c0392b;
}
.card-form__actions {
  margin-top: 18px;
}

/* 버튼 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 22px;
  border-radius: var(--hk-radius-cta);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.16s ease, opacity 0.16s ease;
}
.btn--sm {
  height: 34px;
  padding: 0 14px;
  font-size: 12.5px;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--solid {
  background: var(--hk-ink);
  color: var(--hk-on-dark);
  border: 1px solid var(--hk-ink);
}
.btn--solid:not(:disabled):hover {
  transform: translateY(-1px);
}
.btn--ghost {
  background: var(--hk-surface);
  color: var(--hk-ink);
  border: 1px solid var(--hk-border);
}
.link-btn {
  background: none;
  border: none;
  padding: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--hk-text-quiet);
  cursor: pointer;
  transition: color 0.16s ease;
}
.link-btn:hover {
  color: var(--hk-ink);
}
.link-btn--danger:hover {
  color: #c0392b;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}

/* ── 목록 ── */
.list__title {
  font-size: 1.05rem;
  font-weight: 700;
  margin-bottom: 16px;
}
.list__count {
  margin-left: 6px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--hk-text-quiet);
}
.list__center,
.list__empty {
  padding: 40px 0;
  font-size: 0.9rem;
  color: var(--hk-text-quiet);
  text-align: center;
}
.list__items {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--hk-border);
}
.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--hk-border);
}
.row--inactive {
  opacity: 0.55;
}
.row__thumb {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
  border-radius: var(--hk-radius-cta);
  overflow: hidden;
  background: var(--hk-cream);
  display: flex;
  align-items: center;
  justify-content: center;
}
.row__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.row__thumb--empty {
  color: var(--hk-text-quiet);
  font-size: 22px;
}
.row__main {
  flex: 1 1 auto;
  min-width: 0;
}
.row__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}
.row__order {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  color: var(--hk-text-quiet);
}
.row__badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-cream);
  color: var(--hk-text-quiet);
}
.row__dest {
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--hk-text-quiet);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.16s ease;
}
.row__dest:hover {
  color: var(--hk-ink);
}
.row__name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row__sub {
  font-size: 12.5px;
  color: var(--hk-text-quiet);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
}

@media (max-width: 560px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
