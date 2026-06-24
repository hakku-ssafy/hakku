<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 토스 failUrl 로 전달되는 실패 사유(code/message).
const code = computed(() => String(route.query.code ?? ''))
const message = computed(() =>
  String(route.query.message ?? '결제가 취소되었거나 실패했습니다.'),
)
</script>

<template>
  <div class="u-container pay-result">
    <div class="pay-result__icon">!</div>
    <h1 class="pay-result__title">결제 실패</h1>
    <p class="pay-result__desc">{{ message }}</p>
    <p v-if="code" class="pay-result__code">코드: {{ code }}</p>
    <router-link to="/cart" class="pay-result__cta">장바구니로 돌아가기</router-link>
  </div>
</template>

<style scoped>
.pay-result {
  max-width: 480px;
  padding: 80px 20px 100px;
  text-align: center;
}
.pay-result__icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fdecea;
  color: #c0392b;
  font-size: 28px;
  font-weight: 700;
}
.pay-result__title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 12px;
}
.pay-result__desc {
  font-size: 14px;
  color: var(--hk-text-muted);
  margin-bottom: 8px;
}
.pay-result__code {
  font-size: 12px;
  color: var(--hk-text-quiet);
  font-family: var(--hk-font-mono, monospace);
  margin-bottom: 28px;
}
.pay-result__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  padding: 0 28px;
  border-radius: var(--hk-radius-cta);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
</style>
