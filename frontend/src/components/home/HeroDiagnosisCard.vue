<template>
  <!-- 비로그인 -->
  <div v-if="state === 'guest'" class="dcard dcard--dark dcard--guest">
    <div>
      <span class="dcard__eyebrow dcard__eyebrow--light">My Color · 진단</span>
      <h2 class="dcard__title">나에게 어울리는<br />색 찾기</h2>
      <p class="dcard__desc dcard__desc--light">사진 한 장으로 16가지 퍼스널컬러를 분석해드려요.</p>
    </div>
    <div class="dcard__swatches">
      <span style="background: #e8a07e" /><span style="background: #d98ba6" />
      <span style="background: #8e7bc4" /><span style="background: #afd8c8" />
    </div>
    <router-link to="/diagnosis" class="dcard__cta dcard__cta--light">무료로 진단 받기 →</router-link>
  </div>

  <!-- 미진단(NONE) -->
  <div v-else-if="state === 'none'" class="dcard dcard--dark dcard--none">
    <div>
      <span class="dcard__eyebrow dcard__eyebrow--light">Step 01 · 진단</span>
      <h2 class="dcard__title">아직 내<br />퍼스널컬러를<br />모르고 있어요</h2>
    </div>
    <div class="dcard__swatches">
      <span style="background: #e8a07e" /><span style="background: #d98ba6" /><span style="background: #8e7bc4" />
    </div>
    <router-link to="/diagnosis" class="dcard__cta dcard__cta--light">지금 진단받기 →</router-link>
  </div>

  <!-- 분석 중(PENDING) -->
  <div v-else-if="state === 'pending'" class="dcard dcard--tint dcard--pending">
    <span class="dcard__spinner" aria-hidden="true" />
    <div>
      <span class="dcard__eyebrow dcard__eyebrow--accent">Analyzing</span>
      <h2 class="dcard__title dcard__title--ink">AI가 분석하고<br />있어요</h2>
      <p class="dcard__desc">완료되면 알림으로 알려드릴게요.</p>
    </div>
  </div>

  <!-- 진단 완료(COMPLETED) -->
  <div v-else class="dcard dcard--tint dcard--done">
    <div>
      <span class="dcard__eyebrow dcard__eyebrow--accent-ink">Your Personal Color</span>
      <h2 class="dcard__title dcard__title--accent-ink">{{ personalColorLabel }}</h2>
      <p class="dcard__desc">이 컬러 기준으로 어울리는 아이템을 골라뒀어요.</p>
    </div>
    <div class="dcard__accent-block" aria-hidden="true" />
    <div class="dcard__actions">
      <button v-if="hasDiagnosisImage" type="button" class="dcard__btn dcard__btn--outline" @click="$emit('view-image')">
        진단 이미지
      </button>
      <router-link to="/recommendations" class="dcard__btn dcard__btn--accent">맞춤 상품 →</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  state: 'guest' | 'none' | 'pending' | 'done'
  personalColorLabel?: string
  hasDiagnosisImage?: boolean
}
withDefaults(defineProps<Props>(), { personalColorLabel: '', hasDiagnosisImage: false })
defineEmits<{ 'view-image': [] }>()
</script>

<style scoped>
.dcard {
  position: relative;
  overflow: hidden;
  height: 100%;
  box-sizing: border-box;
  border-radius: var(--hk-radius-md);
  padding: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.dcard--dark {
  color: var(--hk-on-dark);
}
.dcard--guest {
  background: linear-gradient(155deg, var(--hk-dark-2), var(--hk-dark-4));
}
.dcard--none {
  background: linear-gradient(155deg, var(--hk-dark), var(--hk-dark-4));
}
.dcard--tint {
  background: linear-gradient(155deg, var(--accent-soft, #f1efec), var(--hk-surface-warm));
}
.dcard--pending {
  border: 1px solid var(--hk-border-foryou);
  justify-content: center;
  align-items: flex-start;
  gap: 20px;
}
.dcard--done {
  border: 1px solid var(--accent, #16140f);
}

.dcard__eyebrow {
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}
.dcard__eyebrow--light {
  color: rgba(244, 239, 230, 0.6);
}
.dcard__eyebrow--accent {
  letter-spacing: 0.22em;
  color: var(--accent, #16140f);
}
.dcard__eyebrow--accent-ink {
  letter-spacing: 0.2em;
  color: var(--accent-ink, #16140f);
}

.dcard__title {
  margin: 14px 0 0;
  font-size: 30px;
  line-height: 1.18;
  letter-spacing: -0.03em;
  font-weight: 800;
}
.dcard__title--ink {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--hk-ink);
}
.dcard__title--accent-ink {
  font-size: 34px;
  line-height: 1.1;
  color: var(--accent-ink, #16140f);
}

.dcard__desc {
  margin: 13px 0 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--hk-text-muted);
}
.dcard__desc--light {
  color: #c4bcae;
}

.dcard__swatches {
  display: flex;
  gap: 8px;
}
.dcard__swatches span {
  flex: 1;
  height: 54px;
  border-radius: var(--hk-radius-md);
}

.dcard__cta {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  border-radius: var(--hk-radius-pill);
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.18s var(--ease-out-expo), opacity 0.18s ease;
}
.dcard__cta--light {
  background: #fff;
  color: var(--hk-ink);
}
.dcard__cta:hover {
  transform: translateY(-1px);
  opacity: 0.94;
}

.dcard__spinner {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 4px solid rgba(0, 0, 0, 0.08);
  border-top-color: var(--accent, #16140f);
  animation: hk-spin 0.8s linear infinite;
}

.dcard__accent-block {
  height: 96px;
  border-radius: var(--hk-radius-block);
  background: var(--accent, #16140f);
  box-shadow: var(--hk-shadow-card);
}

.dcard__actions {
  display: flex;
  gap: 8px;
}
.dcard__btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border-radius: var(--hk-radius-pill);
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.18s var(--ease-out-expo), opacity 0.18s ease;
}
.dcard__btn:hover {
  transform: translateY(-1px);
  opacity: 0.94;
}
.dcard__btn--outline {
  background: #fff;
  border: 1px solid var(--accent, #16140f);
  color: var(--accent-ink, #16140f);
}
.dcard__btn--accent {
  background: var(--accent, #16140f);
  color: #fff;
}

@media (prefers-reduced-motion: reduce) {
  .dcard__spinner {
    animation: none;
  }
}

/* 모바일 — 카드 패딩/타이포 축소 */
@media (max-width: 767.98px) {
  .dcard {
    padding: 24px;
  }
  .dcard__title {
    font-size: 26px;
  }
  .dcard__title--accent-ink {
    font-size: 30px;
  }
}
</style>
