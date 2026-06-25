<template>
  <main class="u-container admin-products">
    <header class="admin-products__head">
      <p class="admin-products__eyebrow">ADMIN · 상품 관리</p>
      <h1 class="admin-products__title">상품 인라인 편집</h1>
      <p class="admin-products__sub">
        한 페이지에서 활성화 여부·태그·이름을 바로 수정합니다. 스크롤을 내리면 커서 방식으로 더 불러옵니다.
      </p>
    </header>

    <p v-if="error" class="admin-products__error" role="alert">{{ error }}</p>

    <ul class="prod-list">
      <li v-for="row in rows" :key="row.id" class="prod-row" :class="{ 'prod-row--off': !row.active }">
        <div class="prod-row__thumb">
          <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.name" loading="lazy" />
          <span v-else class="prod-row__noimg" aria-hidden="true">No image</span>
        </div>

        <div class="prod-row__fields">
          <label class="field">
            <span class="field__label">이름</span>
            <input v-model="row.name" type="text" class="field__input" />
          </label>

          <label class="field field--cat">
            <span class="field__label">카테고리</span>
            <select v-model="row.category" class="field__input">
              <option value="">미분류</option>
              <option v-for="cat in PRODUCT_CATEGORIES" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </label>

          <label class="field field--tags">
            <span class="field__label">태그(쉼표로 구분)</span>
            <input v-model="row.stylesText" type="text" class="field__input" placeholder="예) 빈티지, 러블리" />
          </label>
        </div>

        <div class="prod-row__actions">
          <label class="toggle" :class="{ 'toggle--on': row.active }">
            <input type="checkbox" v-model="row.active" class="toggle__input" />
            <span class="toggle__track" aria-hidden="true"></span>
            <span class="toggle__text">활성화</span>
          </label>
          <button type="button" class="btn-save" :disabled="row.saving" @click="save(row)">
            {{ row.saving ? '저장 중…' : '저장' }}
          </button>
          <span v-if="row.saved" class="saved-flag" aria-live="polite">저장됨 ✓</span>
        </div>
      </li>
    </ul>

    <div ref="sentinel" class="admin-products__sentinel" aria-hidden="true"></div>
    <p v-if="loading" class="admin-products__status">불러오는 중…</p>
    <p v-else-if="!hasMore && rows.length > 0" class="admin-products__status">— 마지막 상품입니다 —</p>
    <p v-else-if="!loading && rows.length === 0" class="admin-products__status">등록된 상품이 없습니다.</p>
  </main>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { listAdminProducts, editAdminProduct } from '@/api/products'
import { PRODUCT_CATEGORIES, type Product } from '@/types'

interface EditableRow {
  id: number
  imageUrl: string | null
  name: string
  category: string
  stylesText: string
  active: boolean
  saving: boolean
  saved: boolean
}

const PAGE_SIZE = 20

const rows = ref<EditableRow[]>([])
const cursor = ref<number | undefined>(undefined)
const hasMore = ref(true)
const loading = ref(false)
const error = ref<string | null>(null)
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

function toRow(product: Product): EditableRow {
  return {
    id: product.id,
    imageUrl: product.imageUrl,
    name: product.name,
    category: product.category ?? '',
    stylesText: product.styles.join(', '),
    active: product.active ?? true,
    saving: false,
    saved: false,
  }
}

function parseTags(text: string): string[] {
  return text
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

async function loadMore(): Promise<void> {
  if (loading.value || !hasMore.value) return
  loading.value = true
  error.value = null
  try {
    const page = await listAdminProducts(cursor.value, PAGE_SIZE)
    rows.value.push(...page.items.map(toRow))
    cursor.value = page.nextCursor
    hasMore.value = page.hasMore
  } catch {
    error.value = '상품 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function save(row: EditableRow): Promise<void> {
  row.saving = true
  row.saved = false
  error.value = null
  try {
    const updated = await editAdminProduct(row.id, {
      name: row.name.trim(),
      category: row.category ? row.category : null,
      styles: parseTags(row.stylesText),
      active: row.active,
    })
    row.name = updated.name
    row.category = updated.category ?? ''
    row.stylesText = updated.styles.join(', ')
    row.active = updated.active ?? true
    row.saved = true
  } catch {
    error.value = '저장에 실패했습니다.'
  } finally {
    row.saving = false
  }
}

function onIntersect(entries: IntersectionObserverEntry[]): void {
  if (entries[0]?.isIntersecting) void loadMore()
}

onMounted(async () => {
  await loadMore()
  observer = new IntersectionObserver(onIntersect, { rootMargin: '240px' })
  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => observer?.disconnect())
</script>

<style scoped>
.admin-products {
  padding-top: clamp(24px, 4vw, 48px);
  padding-bottom: 96px;
}
.admin-products__head {
  max-width: 60ch;
  margin-bottom: 28px;
}
.admin-products__eyebrow {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--hk-text-quiet);
}
.admin-products__title {
  margin: 8px 0 6px;
  font-size: clamp(1.6rem, 1.2rem + 1.6vw, 2.4rem);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--hk-ink);
}
.admin-products__sub {
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--hk-text-muted);
}
.admin-products__error {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: var(--hk-radius-md);
  background: #fdecec;
  color: #a11;
  font-size: 13px;
}

.prod-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.prod-row {
  display: grid;
  grid-template-columns: 72px 1fr auto;
  gap: 18px;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--hk-border-control);
  border-radius: var(--hk-radius-md);
  background: var(--hk-surface);
  transition: border-color 0.18s ease, opacity 0.18s ease, box-shadow 0.18s ease;
}
.prod-row:hover {
  border-color: var(--hk-ink);
  box-shadow: var(--hk-shadow-card);
}
.prod-row--off {
  opacity: 0.58;
  background: var(--hk-surface-warm, #faf7f2);
}
.prod-row__thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--hk-radius-md);
  overflow: hidden;
  background: var(--hk-cream, #f1ede6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.prod-row__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.prod-row__noimg {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--hk-text-quiet);
}

.prod-row__fields {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.field__label {
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--hk-text-quiet);
}
.field__input {
  height: 38px;
  padding: 0 10px;
  border: 1px solid var(--hk-border-control);
  border-radius: var(--hk-radius-sm, 8px);
  background: #fff;
  font-size: 13.5px;
  color: var(--hk-ink);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.field__input:focus {
  outline: none;
  border-color: var(--accent, #16140f);
  box-shadow: 0 0 0 3px rgba(22, 20, 15, 0.08);
}

.prod-row__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.toggle__input {
  position: absolute;
  opacity: 0;
  width: 1px;
  height: 1px;
}
.toggle__track {
  position: relative;
  width: 38px;
  height: 22px;
  border-radius: var(--hk-radius-pill);
  background: var(--hk-border-control);
  transition: background 0.18s ease;
}
.toggle__track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.18s var(--ease-out-expo, ease);
}
.toggle--on .toggle__track {
  background: var(--accent, #16140f);
}
.toggle--on .toggle__track::after {
  transform: translateX(16px);
}
.toggle__input:focus-visible + .toggle__track {
  box-shadow: 0 0 0 3px rgba(22, 20, 15, 0.18);
}
.toggle__text {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hk-text-muted);
}

.btn-save {
  height: 38px;
  padding: 0 18px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--hk-ink);
  background: var(--hk-ink);
  color: var(--hk-on-dark, #fff);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.16s var(--ease-out-expo, ease), opacity 0.16s ease;
}
.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
}
.btn-save:disabled {
  opacity: 0.5;
  cursor: progress;
}
.saved-flag {
  font-size: 12px;
  font-weight: 600;
  color: var(--hk-success, #2c7);
}

.admin-products__sentinel {
  height: 1px;
}
.admin-products__status {
  margin-top: 20px;
  text-align: center;
  font-size: 12.5px;
  letter-spacing: 0.04em;
  color: var(--hk-text-quiet);
}

@media (max-width: 860px) {
  .prod-row {
    grid-template-columns: 56px 1fr;
    grid-template-areas: 'thumb fields' 'actions actions';
    row-gap: 12px;
  }
  .prod-row__thumb {
    grid-area: thumb;
    width: 56px;
    height: 56px;
  }
  .prod-row__fields {
    grid-area: fields;
    grid-template-columns: 1fr;
  }
  .prod-row__actions {
    grid-area: actions;
    justify-content: space-between;
  }
}
</style>
