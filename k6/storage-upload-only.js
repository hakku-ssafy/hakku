import http from 'k6/http'
import { check, sleep } from 'k6'
import { pickImagePayload } from './lib/image-payload.js'

const STORAGE_URL = __ENV.STORAGE_URL || 'http://localhost:19001'

export const options = { vus: 5, duration: '15s' }

export default function () {
  const p = pickImagePayload()
  const res = http.post(
    `${STORAGE_URL}/storage/images?kind=${p.kind}`,
    p.data,
    { headers: { 'Content-Type': p.contentType } },
  )
  check(res, { '201': (r) => r.status === 201 })
  sleep(0.2)
}
