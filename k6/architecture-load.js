/**
 * 학꾸 전체 아키텍처 부하 테스트 (AI 제외)
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:19001'
const STORAGE_URL = __ENV.STORAGE_URL || BASE_URL
const TEST_TOKEN = __ENV.TEST_JWT || ''
const RUN_LABEL = __ENV.RUN_LABEL || 'run'

const productLatency = new Trend('product_list_latency', true)
const recommendLatency = new Trend('recommendation_latency', true)
const notificationLatency = new Trend('notification_latency', true)
const uploadLatency = new Trend('image_upload_latency', true)
const errorRate = new Rate('error_rate')
const scenarioCounter = new Counter('scenario_total')

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    error_rate: ['rate<0.05'],
  },
}

export default function () {
  const roll = Math.random()

  if (roll < 0.45) {
    scenarioCounter.add(1, { scenario: 'products' })
    const res = http.get(`${BASE_URL}/api/products`)
    const ok = check(res, { 'products: 200': (r) => r.status === 200 })
    productLatency.add(res.timings.duration)
    errorRate.add(!ok)
  } else if (roll < 0.60 && TEST_TOKEN) {
    scenarioCounter.add(1, { scenario: 'recommendations' })
    const res = http.get(`${BASE_URL}/api/recommendations`, {
      headers: { Authorization: `Bearer ${TEST_TOKEN}` },
    })
    const ok = check(res, { 'recommendations: 200': (r) => r.status === 200 })
    recommendLatency.add(res.timings.duration)
    errorRate.add(!ok)
  } else if (roll < 0.75 && TEST_TOKEN) {
    scenarioCounter.add(1, { scenario: 'notifications' })
    const res = http.get(`${BASE_URL}/api/notifications`, {
      headers: { Authorization: `Bearer ${TEST_TOKEN}` },
    })
    const ok = check(res, { 'notifications: 200': (r) => r.status === 200 })
    notificationLatency.add(res.timings.duration)
    errorRate.add(!ok)
  } else {
    scenarioCounter.add(1, { scenario: 'storage_upload' })
    const res = http.post(
      `${STORAGE_URL}/storage/images?kind=raw`,
      '0'.repeat(1024),
      { headers: { 'Content-Type': 'application/octet-stream' } },
    )
    const ok = check(res, { 'upload: 201': (r) => r.status === 201 })
    uploadLatency.add(res.timings.duration)
    errorRate.add(!ok)
  }

  sleep(0.5 + Math.random() * 0.5)
}

function metric(data, name, field) {
  const m = data.metrics[name]
  if (!m || !m.values) return null
  return m.values[field]
}

export function handleSummary(data) {
  const summary = {
    label: RUN_LABEL,
    storageUrl: STORAGE_URL,
    baseUrl: BASE_URL,
    httpReqs: metric(data, 'http_reqs', 'count'),
    httpReqFailedRate: metric(data, 'http_req_failed', 'rate'),
    httpDurationAvg: metric(data, 'http_req_duration', 'avg'),
    httpDurationP95: metric(data, 'http_req_duration', 'p(95)'),
    httpDurationP99: metric(data, 'http_req_duration', 'p(99)'),
    productP95: metric(data, 'product_list_latency', 'p(95)'),
    recommendP95: metric(data, 'recommendation_latency', 'p(95)'),
    notificationP95: metric(data, 'notification_latency', 'p(95)'),
    uploadP95: metric(data, 'image_upload_latency', 'p(95)'),
    uploadAvg: metric(data, 'image_upload_latency', 'avg'),
    errorRate: metric(data, 'error_rate', 'rate'),
  }

  console.log(`\n=== [${RUN_LABEL}] 아키텍처 부하 테스트 ===`)
  console.log(`Storage URL: ${STORAGE_URL}`)
  console.log(`총 요청: ${summary.httpReqs}`)
  console.log(`오류율: ${((summary.httpReqFailedRate || 0) * 100).toFixed(2)}%`)
  console.log(`전체 P95: ${(summary.httpDurationP95 || 0).toFixed(0)}ms`)
  console.log(`Storage 업로드 P95: ${(summary.uploadP95 || 0).toFixed(0)}ms`)

  return {
    [`results-${RUN_LABEL}.json`]: JSON.stringify(summary, null, 2),
  }
}
