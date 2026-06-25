import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { render, screen, fireEvent } from '@testing-library/vue'
import AppInput from '../AppInput.vue'

// v-model 양방향 바인딩을 실제로 재현하기 위한 래퍼.
function mountWithModel(props: Record<string, unknown> = {}, initial = '') {
  const model = ref(initial)
  render({
    components: { AppInput },
    setup: () => ({ model, props }),
    template: '<AppInput v-model="model" label="비밀번호" v-bind="props" />',
  })
  return model
}

describe('AppInput latin-only', () => {
  it('latin-only가 없으면 한글 입력을 그대로 둔다', async () => {
    const model = mountWithModel()
    await fireEvent.update(screen.getByLabelText('비밀번호'), '비밀')
    expect(model.value).toBe('비밀')
  })

  it('latin-only면 한글 입력을 두벌식 영문으로 변환한다', async () => {
    const model = mountWithModel({ latinOnly: true })
    // IME 조합 완료 후 model이 갱신되는 상황을 update 이벤트로 재현
    await fireEvent.update(screen.getByLabelText('비밀번호'), '비밀')
    expect(model.value).toBe('qlalf')
  })

  it('latin-only면 영문 입력은 변경하지 않는다', async () => {
    const model = mountWithModel({ latinOnly: true })
    await fireEvent.update(screen.getByLabelText('비밀번호'), 'pass123')
    expect(model.value).toBe('pass123')
  })

  it('변환 결과가 입력창에도 반영된다', async () => {
    mountWithModel({ latinOnly: true })
    const input = screen.getByLabelText('비밀번호') as HTMLInputElement
    await fireEvent.update(input, '안녕')
    expect(input.value).toBe('dkssud')
  })
})
