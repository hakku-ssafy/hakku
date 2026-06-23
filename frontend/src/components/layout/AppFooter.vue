<template>
  <footer class="border-t border-line bg-footer mt-20">
    <div class="u-container py-12">
      <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-6">
        <div>
          <p class="logo-word leading-none text-ink">hakku<span class="text-accent">.</span></p>
          <p class="text-sm text-ink-muted mt-3 max-w-xs leading-relaxed">
            AI 퍼스널컬러 진단으로 나에게 어울리는 색을 찾고, 학생증 꾸미기 아이템을 추천받는 커머스·커뮤니티.
          </p>
        </div>
        <nav class="flex flex-wrap gap-x-6 gap-y-2 text-sm text-ink-soft" aria-label="푸터 메뉴">
          <router-link to="/products" class="hover:text-ink transition-colors">상품</router-link>
          <router-link to="/community" class="hover:text-ink transition-colors">커뮤니티</router-link>
          <router-link to="/diagnosis" class="hover:text-ink transition-colors">퍼스널컬러 진단</router-link>
        </nav>
      </div>
      <p class="text-xs text-ink-faint mt-10 pt-6 border-t border-line">
        © 2026 학꾸 (Hakku). AI 퍼스널컬러 커머스.
      </p>
    </div>
  </footer>
</template>

<style scoped>
.logo-word {
  font-weight: 800;
  font-size: 1.4375rem; /* 23px */
  letter-spacing: -0.04em;
}
</style>
