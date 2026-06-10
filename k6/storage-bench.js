/**
 * Storage 서버 단독 벤치마크 (Go vs Spring 비교용)
 *
 * 각 iteration: 실제 이미지 업로드 → 다운로드 → 삭제
 * 사용법:
 *   k6 run -e STORAGE_URL=http://localhost:8081 -e RUN_LABEL=go storage-bench.js
 *   k6 run -e STORAGE_URL=http://localhost:8082 -e RUN_LABEL=spring storage-bench.js
 */
import http from 'k6/http'
import { check } from 'k6'
import { Trend, Rate } from 'k6/metrics'
import { pickImagePayload, fixtureSummary } from './lib/image-payload.js'

const STORAGE_URL = __ENV.STORAGE_URL || 'http://localhost:8081'
const RUN_LABEL = __ENV.RUN_LABEL || 'run'
const OUT_DIR = __ENV.OUT_DIR || '.'
const VUS = Number(__ENV.VUS || 20)
const DURATION = __ENV.DURATION || '60s'

const uploadLatency = new Trend('upload_latency', true)
const downloadLatency = new Trend('download_latency', true)
const deleteLatency = new Trend('delete_latency', true)
const errorRate = new Rate('error_rate')

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    storage: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
}

export default function () {
  const payload = pickImagePayload()

  const up = http.post(
    `${STORAGE_URL}/storage/images?kind=${payload.kind}`,
    payload.data,
    { headers: { 'Content-Type': payload.contentType }, tags: { op: 'upload' } },
  )
  const upOk = check(up, { 'upload 201': (r) => r.status === 201 })
  uploadLatency.add(up.timings.duration)
  errorRate.add(!upOk)
  if (!upOk) return

  const id = up.json('id')

  const down = http.get(`${STORAGE_URL}/storage/images/${id}`, {
    responseType: 'binary',
    tags: { op: 'download' },
  })
  const downOk = check(down, {
    'download 200': (r) => r.status === 200,
    'download size': (r) => r.body.byteLength === payload.sizeBytes,
  })
  downloadLatency.add(down.timings.duration)
  errorRate.add(!downOk)

  const del = http.del(`${STORAGE_URL}/storage/images/${id}`, null, { tags: { op: 'delete' } })
  const delOk = check(del, { 'delete 204': (r) => r.status === 204 })
  deleteLatency.add(del.timings.duration)
  errorRate.add(!delOk)
}

function metric(data, name, field) {
  const m = data.metrics[name]
  if (!m || !m.values) return null
  return m.values[field]
}

export function handleSummary(data) {
  const durationSec = data.state.testRunDurationMs / 1000
  const httpReqs = metric(data, 'http_reqs', 'count')
  const summary = {
    label: RUN_LABEL,
    storageUrl: STORAGE_URL,
    vus: VUS,
    durationSec,
    httpReqs,
    rps: httpReqs / durationSec,
    iterations: metric(data, 'iterations', 'count'),
    iterPerSec: metric(data, 'iterations', 'count') / durationSec,
    errorRate: metric(data, 'error_rate', 'rate'),
    httpReqFailedRate: metric(data, 'http_req_failed', 'rate'),
    uploadAvg: metric(data, 'upload_latency', 'avg'),
    uploadP95: metric(data, 'upload_latency', 'p(95)'),
    uploadP99: metric(data, 'upload_latency', 'p(99)'),
    downloadAvg: metric(data, 'download_latency', 'avg'),
    downloadP95: metric(data, 'download_latency', 'p(95)'),
    downloadP99: metric(data, 'download_latency', 'p(99)'),
    deleteAvg: metric(data, 'delete_latency', 'avg'),
    deleteP95: metric(data, 'delete_latency', 'p(95)'),
    dataSentMB: metric(data, 'data_sent', 'count') / 1024 / 1024,
    dataRecvMB: metric(data, 'data_received', 'count') / 1024 / 1024,
    fixtures: fixtureSummary(),
  }
  return {
    stdout: JSON.stringify(summary, null, 2) + '\n',
    [`${OUT_DIR}/results-${RUN_LABEL}.json`]: JSON.stringify(summary, null, 2),
  }
}
