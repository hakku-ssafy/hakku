<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { loadTossPayments, ANONYMOUS } from '@tosspayments/tosspayments-sdk'
import { prepareTossPayment } from '@/api/payments'

// 공개(클라이언트) 키 — 노출돼도 안전. 미설정 시 토스 공개 문서용 샌드박스 키로 폴백.
const CLIENT_KEY =
  import.meta.env.VITE_TOSS_CLIENT_KEY ?? 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm'

type TossWidgets = ReturnType<Awaited<ReturnType<typeof loadTossPayments>>['widgets']>

const route = useRoute()

const refType = String(route.query.refType ?? 'PRODUCT')
const refId = String(route.query.refId ?? '')
const amount = Number(route.query.amount ?? 0)
const orderName = String(route.query.name ?? '주문')

const loading = ref(true)
const ready = ref(false)
const requesting = ref(false)
const errorMessage = ref('')

let widgets: TossWidgets | null = null
let orderId = ''

onMounted(async () => {
  if (!refId || !(amount > 0)) {
    errorMessage.value = '결제 정보가 올바르지 않습니다.'
    loading.value = false
    return
  }
  try {
    // 1) 서버에 의도 선등록 → orderId
    const prepared = await prepareTossPayment({ referenceType: refType, referenceId: refId, amount })
    orderId = prepared.orderId
    // 2) 위젯 초기화 + 렌더 (대상 div 는 v-show 로 항상 DOM 에 존재)
    const tossPayments = await loadTossPayments(CLIENT_KEY)
    widgets = tossPayments.widgets({ customerKey: ANONYMOUS })
    await widgets.setAmount({ currency: 'KRW', value: prepared.amount })
    await Promise.all([
      widgets.renderPaymentMethods({ selector: '#toss-payment-method', variantKey: 'DEFAULT' }),
      widgets.renderAgreement({ selector: '#toss-agreement', variantKey: 'AGREEMENT' }),
    ])
    ready.value = true
  } catch {
    errorMessage.value = '결제 위젯을 불러오지 못했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
})

async function pay() {
  if (!ready.value || requesting.value || !widgets) return
  requesting.value = true
  try {
    // 인증 성공 시 successUrl, 실패 시 failUrl 로 리다이렉트된다(쿼리로 paymentKey/orderId/amount 전달).
    await widgets.requestPayment({
      orderId,
      orderName,
      successUrl: `${window.location.origin}/payments/success`,
      failUrl: `${window.location.origin}/payments/fail`,
    })
  } catch {
    requesting.value = false
    errorMessage.value = '결제 요청에 실패했어요.'
  }
}
</script>

<template>
  <div class="u-container checkout">
    <router-link to="/cart" class="checkout__back">← 돌아가기</router-link>
    <h1 class="checkout__title">결제</h1>

    <p v-if="loading" class="checkout__status" role="status">결제 위젯을 불러오는 중…</p>
    <p v-else-if="errorMessage" class="checkout__error" role="alert">{{ errorMessage }}</p>

    <!-- 위젯 대상 div 는 v-show 로 항상 마운트(렌더 시점에 존재해야 함) -->
    <div v-show="ready" class="checkout__body">
      <div class="checkout__summary">
        <span class="checkout__order">{{ orderName }}</span>
        <strong class="checkout__amount">{{ amount.toLocaleString('ko-KR') }}원</strong>
      </div>
      <div id="toss-payment-method" class="checkout__widget"></div>
      <div id="toss-agreement" class="checkout__widget"></div>
      <button type="button" class="checkout__pay" :disabled="requesting" @click="pay">
        {{ amount.toLocaleString('ko-KR') }}원 결제하기
      </button>
      <p class="checkout__sandbox">샌드박스 결제 — 실제로 청구되지 않습니다.</p>
    </div>
  </div>
</template>

<style scoped>
.checkout {
  padding-top: 28px;
  padding-bottom: 90px;
  max-width: 640px;
}
.checkout__back {
  display: inline-block;
  font-size: 13px;
  color: var(--hk-text-muted-2);
  margin-bottom: 20px;
}
.checkout__title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 22px;
}
.checkout__status,
.checkout__error {
  padding: 40px 0;
  text-align: center;
  font-size: 14px;
  color: var(--hk-text-muted);
}
.checkout__error {
  color: #c0392b;
}
.checkout__summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 16px 0 20px;
  border-bottom: 1px solid var(--hk-border);
  margin-bottom: 16px;
}
.checkout__order {
  font-size: 15px;
  font-weight: 600;
}
.checkout__amount {
  font-size: 20px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.checkout__widget {
  margin-bottom: 8px;
}
.checkout__pay {
  width: 100%;
  height: 54px;
  margin-top: 12px;
  border: none;
  border-radius: var(--hk-radius-cta);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.18s ease;
}
.checkout__pay:hover:not(:disabled) {
  filter: brightness(0.95);
}
.checkout__pay:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.checkout__sandbox {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--hk-text-quiet);
}
</style>
