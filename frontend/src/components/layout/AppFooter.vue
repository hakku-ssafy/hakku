<template>
  <footer class="border-t border-line bg-footer mt-20">
    <div class="u-container footer-main">
      <div class="footer-brand">
        <p class="logo-word leading-none text-ink">hakku<span class="text-accent">.</span></p>
        <p class="footer-intro">AI 퍼스널컬러로 완성하는 나만의 학생증 꾸미기 — 커머스 &amp; 커뮤니티</p>
      </div>

      <div class="footer-cols">
        <nav class="footer-col" aria-label="쇼핑 메뉴">
          <span class="footer-label">SHOP</span>
          <router-link to="/products" class="footer-link">상품</router-link>
          <router-link to="/recommendations" class="footer-link">맞춤 추천</router-link>
        </nav>
        <nav class="footer-col" aria-label="라운지 메뉴">
          <span class="footer-label">LOUNGE</span>
          <router-link to="/community" class="footer-link">커뮤니티</router-link>
          <router-link to="/diagnosis" class="footer-link">퍼스널컬러 진단</router-link>
        </nav>
      </div>
    </div>

    <div class="footer-bar">
      <p class="u-container footer-bar__inner">© 2026 hakku. All rights reserved.</p>
    </div>
  </footer>
</template>

<script setup lang="ts">
// 표시 전용 — 링크 목적지는 기존 라우트를 그대로 사용한다.
</script>

<style scoped>
.logo-word {
  font-weight: 800;
  font-size: 1.375rem; /* 22px */
  letter-spacing: -0.04em;
  margin: 0;
}

.footer-main {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 2.5rem;
  padding-top: 3.125rem; /* 50px */
  padding-bottom: 2.5rem; /* 40px */
}

.footer-intro {
  margin: 0.75rem 0 0;
  max-width: 280px;
  font-size: 0.8125rem; /* 13px */
  line-height: 1.6;
  color: var(--hk-text-muted-2);
}

.footer-cols {
  display: flex;
  gap: 3.5rem; /* 56px */
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 0.5625rem; /* 9px */
}

.footer-label {
  margin-bottom: 0.25rem;
  font-size: 0.6875rem; /* 11px */
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--hk-text-hint);
}

.footer-link {
  font-size: 0.8125rem; /* 13px */
  color: var(--hk-text-muted);
  transition: color 0.18s ease;
}
.footer-link:hover {
  color: var(--hk-ink);
}

.footer-bar {
  border-top: 1px solid var(--hk-border);
}
.footer-bar__inner {
  padding-top: 1.125rem; /* 18px */
  padding-bottom: 1.125rem;
  margin: 0;
  font-size: 0.71875rem; /* 11.5px */
  color: var(--hk-text-quiet);
}

/* 모바일 — 브랜드 위, 링크 컬럼 아래로 스택 */
@media (max-width: 639.98px) {
  .footer-main {
    flex-direction: column;
    gap: 1.75rem;
  }
}
</style>
