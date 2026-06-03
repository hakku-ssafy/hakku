/**
 * 학꾸 k6 부하 테스트
 *
 * 실행:
 *   k6 run k6/load-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 k6/load-test.js
 *
 * 시나리오:
 *   - 상품 목록 조회 (GET /api/products)
 *   - 추천 API (GET /api/recommendations) — JWT 필요
 *   - Storage 이미지 업로드 (POST /storage/images)
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate } from 'k6/metrics'
import { pickImagePayload } from './lib/image-payload.js'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const STORAGE_URL = __ENV.STORAGE_URL || 'http://localhost:8081'

const productLatency = new Trend('product_list_latency', true)
const recommendLatency = new Trend('recommendation_latency', true)
const uploadLatency = new Trend('image_upload_latency', true)
const errorRate = new Rate('error_rate')

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // 램프업
    { duration: '1m',  target: 30 },   // 부하 유지
    { duration: '30s', target: 0 },    // 램프다운
  ],
  thresholds: {
    http_req_failed:           ['rate<0.01'],
    http_req_duration:         ['p(95)<500'],
    product_list_latency:      ['p(95)<300'],
    recommendation_latency:    ['p(95)<500'],
    image_upload_latency:      ['p(95)<5000'],
    error_rate:                ['rate<0.01'],
  },
}

const TEST_TOKEN = __ENV.TEST_JWT || ''

export default function () {
  const scenario = Math.random()

  if (scenario < 0.5) {
    const res = http.get(`${BASE_URL}/api/products`)
    const ok = check(res, {
      'products: status 200': (r) => r.status === 200,
      'products: has body': (r) => r.body && r.body.length > 0,
    })
    productLatency.add(res.timings.duration)
    errorRate.add(!ok)

  } else if (scenario < 0.7 && TEST_TOKEN) {
    const res = http.get(`${BASE_URL}/api/recommendations`, {
      headers: { Authorization: `Bearer ${TEST_TOKEN}` },
    })
    const ok = check(res, {
      'recommendations: status 200': (r) => r.status === 200,
    })
    recommendLatency.add(res.timings.duration)
    errorRate.add(!ok)

  } else {
    const payload = pickImagePayload()
    const res = http.post(
      `${STORAGE_URL}/storage/images?kind=${payload.kind}`,
      payload.data,
      { headers: { 'Content-Type': payload.contentType } },
    )
    
    const ok = check(res, {
      'upload: status 201': (r) => r.status === 201,
    })
    uploadLatency.add(res.timings.duration)
    errorRate.add(!ok)
  }

  sleep(0.5 + Math.random() * 0.5)
}

export function handleSummary(data) {
  console.log('\n=== 학꾸 부하 테스트 결과 ===')
  console.log(`총 요청: ${data.metrics.http_reqs.values.count}`)
  console.log(`오류율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`)
  console.log(`P95 응답시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(0)}ms`)
  console.log(`P99 응답시간: ${data.metrics.http_req_duration.values['p(99)'].toFixed(0)}ms`)
  return {}
}
