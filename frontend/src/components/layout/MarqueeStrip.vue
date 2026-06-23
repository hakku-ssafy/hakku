<template>
  <!-- 상단 키워드 마퀴 — 다크 띠 위로 흐르는 브랜드 키워드. 장식 요소이므로
       라이브 영역이 아니며, 복제본은 aria-hidden 으로 중복 낭독을 막는다. -->
  <div class="marquee" role="presentation">
    <div class="marquee__track">
      <div class="marquee__group">
        <template v-for="(word, i) in KEYWORDS" :key="`a-${i}`">
          <span class="marquee__word">{{ word }}</span>
          <span class="marquee__sep" aria-hidden="true">✦</span>
        </template>
      </div>
      <div class="marquee__group" aria-hidden="true">
        <template v-for="(word, i) in KEYWORDS" :key="`b-${i}`">
          <span class="marquee__word">{{ word }}</span>
          <span class="marquee__sep">✦</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 프로토타입(hakku.dc.html 24–34)의 8개 키워드.
const KEYWORDS = [
  '핀뱃지',
  '키링',
  '꾸미기 스티커',
  '퍼스널컬러',
  '그립톡',
  '키캡',
  '다꾸',
  '마스킹테이프',
] as const
</script>

<style scoped>
.marquee {
  overflow: hidden;
  background: var(--hk-dark);
  border-bottom: 1px solid var(--hk-dark-3);
  color: var(--hk-cream);
}

.marquee__track {
  display: flex;
  width: max-content;
  animation: hk-marq 32s linear infinite;
}

.marquee__group {
  display: flex;
  align-items: center;
  gap: 38px;
  padding: 9px 19px;
  white-space: nowrap;
}

.marquee__word {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.marquee__sep {
  color: var(--accent, #16140f);
  font-size: 11px;
  line-height: 1;
}

/* 모션 최소화 — 흐름 정지 + 복제본 숨겨 단일 행 정렬 유지 */
@media (prefers-reduced-motion: reduce) {
  .marquee__track {
    animation: none;
  }
  .marquee__group[aria-hidden='true'] {
    display: none;
  }
}
</style>
