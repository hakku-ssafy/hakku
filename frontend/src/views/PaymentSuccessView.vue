<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { confirmTossPayment } from '@/api/payments'

const route = useRoute()

const state = ref<'confirming' | 'approved' | 'failed'>('confirming')
const message = ref('')
const amount = ref(0)

onMounted(async () => {
  const paymentKey = String(route.query.paymentKey ?? '')
  const orderId = String(route.query.orderId ?? '')
  const amt = Number(route.query.amount ?? 0)
  amount.value = amt

  if (!paymentKey || !orderId || !(amt > 0)) {
    state.value = 'failed'
    message.value = '결제 정보가 올바르지 않습니다.'
    return
  }
  try {
    // 서버가 토스 승인 API 까지 호출해 최종 확정한다(금액 위변조 검증 포함).
    const result = await confirmTossPayment({ paymentKey, orderId, amount: amt })
    if (result.status === 'APPROVED') {
      state.value = 'approved'
    } else {
      state.value = 'failed'
      message.value = '결제가 승인되지 않았습니다.'
    }
  } catch {
    state.value = 'failed'
    message.value = '결제 승인 처리에 실패했습니다. 결제가 되었다면 잠시 후 반영됩니다.'
  }
})
</script>

<template>
  <div class="u-container pay-result">
    <p v-if="state === 'confirming'" class="pay-result__status" role="status">결제를 확인하는 중…</p>

    <template v-else-if="state === 'approved'">
      <div class="pay-result__icon pay-result__icon--ok">✓</div>
      <h1 class="pay-result__title">결제가 완료되었어요</h1>
      <p class="pay-result__amount">{{ amount.toLocaleString('ko-KR') }}원</p>
      <p class="pay-result__sandbox">샌드박스 결제 — 실제로 청구되지 않았습니다.</p>
      <router-link :to="{ path: '/my', query: { tab: 'orders' } }" class="pay-result__cta">주문 내역 보기</router-link>
      <router-link to="/products" class="pay-result__link">쇼핑 계속하기</router-link>
    </template>

    <template v-else>
      <div class="pay-result__icon pay-result__icon--fail">!</div>
      <h1 class="pay-result__title">결제를 완료하지 못했어요</h1>
      <p class="pay-result__desc">{{ message }}</p>
      <router-link to="/cart" class="pay-result__cta">장바구니로 돌아가기</router-link>
    </template>
  </div>
</template>

<style scoped>
.pay-result {
  max-width: 480px;
  padding: 80px 20px 100px;
  text-align: center;
}
.pay-result__status {
  font-size: 15px;
  color: var(--hk-text-muted);
}
.pay-result__icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 28px;
  font-weight: 700;
}
.pay-result__icon--ok {
  background: var(--accent-soft, #eef3ee);
  color: var(--accent, #16140f);
}
.pay-result__icon--fail {
  background: #fdecea;
  color: #c0392b;
}
.pay-result__title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 12px;
}
.pay-result__amount {
  font-size: 24px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  margin-bottom: 8px;
}
.pay-result__sandbox,
.pay-result__desc {
  font-size: 13.5px;
  color: var(--hk-text-muted);
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
.pay-result__link {
  display: block;
  margin-top: 14px;
  font-size: 13.5px;
  color: var(--hk-text-muted);
  text-decoration: underline;
  text-underline-offset: 3px;
}
</style>
