<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder } from '@/api/orders'
import type { ShippingAddress } from '@/types'
import AppInput from '@/components/ui/AppInput.vue'

const route = useRoute()
const router = useRouter()

const itemCount = computed(() => Number(route.query.count ?? 0))
const displayAmount = computed(() => Number(route.query.amount ?? 0))

const form = reactive<ShippingAddress>({
  recipientName: '',
  phone: '',
  postalCode: '',
  address1: '',
  address2: '',
})

const submitting = ref(false)
const errorMessage = ref('')

/** 데모/테스트 편의: 배송지 칸을 테스트용 값으로 한 번에 채운다. */
function autofill() {
  form.recipientName = '홍길동'
  form.phone = '010-1234-5678'
  form.postalCode = '06236'
  form.address1 = '서울 강남구 테헤란로 427'
  form.address2 = '위워크 8층'
  errorMessage.value = ''
}

async function submit() {
  if (!form.recipientName.trim() || !form.phone.trim() || !form.postalCode.trim() || !form.address1.trim()) {
    errorMessage.value = '받는 분·연락처·우편번호·주소를 모두 입력해주세요.'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const order = await createOrder({ ...form })
    router.push({
      path: '/payments/checkout',
      query: {
        refType: 'ORDER',
        refId: String(order.id),
        amount: String(order.totalAmount),
        name: itemCount.value > 0 ? `주문 ${itemCount.value}건` : '주문',
      },
    })
  } catch {
    errorMessage.value = '주문 생성에 실패했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="u-container order-form">
    <router-link to="/cart" class="order-form__back">← 장바구니로</router-link>
    <h1 class="order-form__title">배송지 입력</h1>

    <div class="order-form__summary">
      <span>주문 금액 <span v-if="itemCount" class="u-mono">({{ itemCount }}건)</span></span>
      <strong class="u-mono tabular-nums">{{ displayAmount.toLocaleString('ko-KR') }}원</strong>
    </div>

    <form class="order-form__fields" @submit.prevent="submit">
      <div class="order-form__row">
        <span class="order-form__legend">배송 정보</span>
        <button type="button" class="order-form__autofill" @click="autofill">자동채움</button>
      </div>

      <AppInput v-model="form.recipientName" label="받는 분" placeholder="이름" autocomplete="name" />
      <AppInput v-model="form.phone" label="연락처" placeholder="010-0000-0000" inputmode="tel" autocomplete="tel" />
      <AppInput v-model="form.postalCode" label="우편번호" placeholder="우편번호" inputmode="numeric" autocomplete="postal-code" />
      <AppInput v-model="form.address1" label="주소" placeholder="도로명/지번 주소" autocomplete="address-line1" />
      <AppInput v-model="form.address2" label="상세 주소" placeholder="동/호수 등 (선택)" autocomplete="address-line2" />

      <p v-if="errorMessage" class="order-form__error" role="alert">{{ errorMessage }}</p>

      <button type="submit" class="order-form__submit" :disabled="submitting">
        {{ submitting ? '주문 생성 중…' : '주문하기' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.order-form {
  padding-top: 28px;
  padding-bottom: 90px;
  max-width: 560px;
}
.order-form__back {
  display: inline-block;
  font-size: 13px;
  color: var(--hk-text-muted-2);
  margin-bottom: 20px;
}
.order-form__title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 20px;
}
.order-form__summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 16px 0;
  border-top: 1px solid var(--hk-border);
  border-bottom: 1px solid var(--hk-border);
  margin-bottom: 22px;
  font-size: 14px;
}
.order-form__summary strong {
  font-size: 18px;
  font-weight: 800;
}
.order-form__fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.order-form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.order-form__legend {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--hk-ink);
}
.order-form__autofill {
  height: 32px;
  padding: 0 12px;
  border-radius: var(--hk-radius-pill);
  border: 1px solid var(--accent, #16140f);
  background: var(--accent-soft, #f1efec);
  color: var(--accent-ink, #16140f);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.18s ease;
}
.order-form__autofill:hover {
  filter: brightness(0.97);
}
.order-form__error {
  font-size: 13px;
  color: #c0392b;
}
.order-form__submit {
  height: 54px;
  margin-top: 8px;
  border: none;
  border-radius: var(--hk-radius-cta);
  background: var(--accent, #16140f);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.18s ease;
}
.order-form__submit:hover:not(:disabled) {
  filter: brightness(0.95);
}
.order-form__submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
