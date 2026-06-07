<template>
  <footer class="border-t border-line mt-20">
    <div class="u-container py-12">
      <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-6">
        <div>
          <p class="u-serif text-2xl leading-none">
            <span class="u-gradient-text">학꾸</span>
          </p>
          <p class="text-sm text-ink-muted mt-3 max-w-xs leading-relaxed">
            AI 퍼스널컬러 진단으로 나에게 어울리는 색을 찾고, 학생증 꾸미기 아이템을 추천받는 커머스·커뮤니티.
          </p>
        </div>
        <nav class="flex flex-wrap gap-x-6 gap-y-2 text-sm text-ink-soft" aria-label="푸터 메뉴">
          <router-link to="/products" class="hover:text-accent transition-colors">상품</router-link>
          <router-link to="/community" class="hover:text-accent transition-colors">커뮤니티</router-link>
          <router-link to="/diagnosis" class="hover:text-accent transition-colors">퍼스널컬러 진단</router-link>
        </nav>
      </div>
      <p class="text-xs text-ink-muted mt-10 pt-6 border-t border-line">
        © 2026 학꾸 (Hakku). AI 퍼스널컬러 커머스.
      </p>
    </div>
  </footer>
</template>
