<template>
  <div class="u-container admin">
    <header class="admin__head">
      <span class="u-eyebrow">Admin</span>
      <h1 class="admin__title">큐레이션 카드 관리</h1>
      <p class="admin__desc">메인 캐러셀에 노출되는 프로모션·매거진 카드를 추가·수정·정렬합니다.</p>
    </header>

    <!-- ===== 작성/수정 폼 ===== -->
    <form class="card-form" @submit.prevent="save">
      <div class="card-form__head">
        <h2 class="card-form__title">{{ editingId ? '카드 수정' : '새 카드 추가' }}</h2>
        <button v-if="editingId" type="button" class="link-btn" @click="resetForm">새 카드로 전환</button>
      </div>

      <div class="field-grid">
        <label class="field">
          <span class="field__label">키커 (작은 라벨)</span>
          <input v-model="form.kicker" type="text" class="input" placeholder="NEW ARRIVAL" maxlength="80" />
        </label>
        <label class="field">
          <span class="field__label">정렬 순서</span>
          <input v-model.number="form.displayOrder" type="number" class="input" min="0" />
        </label>
      </div>

      <label class="field">
        <span class="field__label">제목 <em class="req">*</em></span>
        <input v-model="form.title" type="text" class="input" placeholder="이주의 신상 키링 모음" maxlength="200" required />
      </label>

      <label class="field">
        <span class="field__label">문구 (부제)</span>
        <input v-model="form.subtitle" type="text" class="input" placeholder="지금 가장 인기 있는 픽" maxlength="300" />
      </label>

      <label class="field">
        <span class="field__label">이미지</span>
        <div class="uploader">
          <div class="uploader__preview" :class="{ 'uploader__preview--empty': !form.imageUrl }">
            <img v-if="form.imageUrl" :src="form.imageUrl" alt="미리보기" />
            <span v-else aria-hidden="true">◍</span>
          </div>
          <div class="uploader__actions">
            <label class="btn btn--ghost">
              {{ uploading ? '업로드 중…' : '이미지 선택' }}
              <input type="file" accept="image/*" class="sr-only" :disabled="uploading" @change="onImageChange" />
            </label>
            <button v-if="form.imageUrl" type="button" class="link-btn" @click="form.imageUrl = ''">제거</button>
          </div>
        </div>
      </label>

      <label class="field">
        <span class="field__label">링크 URL</span>
        <input v-model="form.linkUrl" type="text" class="input" placeholder="/products 또는 https://…" />
        <span class="field__hint">비워두면 카드를 누를 때 본문(매거진 상세)으로 이동합니다.</span>
      </label>

      <label class="field">
        <span class="field__label">본문 (매거진 콘텐츠)</span>
        <textarea v-model="form.body" class="input textarea" rows="6" placeholder="링크가 없을 때 매거진 상세에 표시될 내용을 작성하세요."></textarea>
      </label>

      <label class="toggle">
        <input v-model="form.active" type="checkbox" />
        <span>메인에 노출 (활성)</span>
      </label>

      <p v-if="errorMsg" class="card-form__error">{{ errorMsg }}</p>

      <div class="card-form__actions">
        <button type="submit" class="btn btn--solid" :disabled="!canSave">
          {{ saving ? '저장 중…' : editingId ? '수정 저장' : '카드 추가' }}
        </button>
      </div>
    </form>

    <!-- ===== 카드 목록 ===== -->
    <section class="list">
      <h2 class="list__title">등록된 카드 <span class="list__count">{{ cards.length }}</span></h2>

      <div v-if="loading" role="status" class="list__center">불러오는 중…</div>
      <p v-else-if="cards.length === 0" class="list__empty">아직 등록된 카드가 없습니다.</p>

      <ul v-else class="list__items">
        <li v-for="card in cards" :key="card.id" class="row" :class="{ 'row--inactive': !card.active }">
          <div class="row__thumb" :class="{ 'row__thumb--empty': !card.imageUrl }">
            <img v-if="card.imageUrl" :src="card.imageUrl" :alt="card.title" />
            <span v-else aria-hidden="true">◍</span>
          </div>
          <div class="row__main">
            <div class="row__top">
              <span class="row__order">#{{ card.displayOrder }}</span>
              <span v-if="!card.active" class="row__badge">비활성</span>
              <span class="row__dest">{{ card.linkUrl ? card.linkUrl : '매거진 상세' }}</span>
            </div>
            <p class="row__name">{{ card.title }}</p>
            <p v-if="card.subtitle" class="row__sub">{{ card.subtitle }}</p>
          </div>
          <div class="row__actions">
            <button type="button" class="link-btn" @click="startEdit(card)">수정</button>
            <button type="button" class="link-btn link-btn--danger" @click="remove(card)">삭제</button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  listAllCurationCards,
  createCurationCard,
  updateCurationCard,
  deleteCurationCard,
} from '@/api/curation'
import { uploadCurationImage } from '@/api/storage'
import type { CurationCard, CurationCardInput } from '@/types'

const cards = ref<CurationCard[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const editingId = ref<number | null>(null)
const errorMsg = ref<string | null>(null)

const form = reactive({
  kicker: '',
  title: '',
  subtitle: '',
  body: '',
  imageUrl: '',
  linkUrl: '',
  displayOrder: 0,
  active: true,
})

const canSave = computed(() => form.title.trim().length > 0 && !saving.value && !uploading.value)

async function load() {
  loading.value = true
  try {
    cards.value = await listAllCurationCards()
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
    body: '',
    imageUrl: '',
    linkUrl: '',
    displayOrder: cards.value.length,
    active: true,
  })
}

function startEdit(card: CurationCard) {
  editingId.value = card.id
  errorMsg.value = null
  Object.assign(form, {
    kicker: card.kicker ?? '',
    title: card.title,
    subtitle: card.subtitle ?? '',
    body: card.body ?? '',
    imageUrl: card.imageUrl ?? '',
    linkUrl: card.linkUrl ?? '',
    displayOrder: card.displayOrder,
    active: card.active,
  })
  if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function onImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  errorMsg.value = null
  try {
    form.imageUrl = await uploadCurationImage(file)
  } catch {
    errorMsg.value = '이미지 업로드에 실패했습니다.'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function buildInput(): CurationCardInput {
  const nz = (s: string) => {
    const t = s.trim()
    return t.length > 0 ? t : null
  }
  return {
    kicker: nz(form.kicker),
    title: form.title.trim(),
    subtitle: nz(form.subtitle),
    body: nz(form.body),
    imageUrl: nz(form.imageUrl),
    linkUrl: nz(form.linkUrl),
    displayOrder: Number(form.displayOrder) || 0,
    active: form.active,
  }
}

async function save() {
  if (!canSave.value) return
  saving.value = true
  errorMsg.value = null
  try {
    const input = buildInput()
    if (editingId.value != null) {
      await updateCurationCard(editingId.value, input)
    } else {
      await createCurationCard(input)
    }
    await load()
    resetForm()
  } catch {
    errorMsg.value = '저장에 실패했습니다. 다시 시도해 주세요.'
  } finally {
    saving.value = false
  }
}

async function remove(card: CurationCard) {
  if (typeof window !== 'undefined' && !window.confirm(`'${card.title}' 카드를 삭제할까요?`)) return
  try {
    await deleteCurationCard(card.id)
    if (editingId.value === card.id) resetForm()
    await load()
  } catch {
    errorMsg.value = '삭제에 실패했습니다.'
  }
}

onMounted(async () => {
  await load()
  form.displayOrder = cards.value.length
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
  color: var(--hk-text-quiet);
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
  line-height: 1.6;
  resize: vertical;
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
  font-size: 11.5px;
  color: var(--hk-text-quiet);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
