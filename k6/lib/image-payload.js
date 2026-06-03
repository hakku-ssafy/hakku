/**
 * Realistic image payloads (paths relative to k6/ root via ../ from lib/).
 */
import { Trend } from 'k6/metrics'

export const uploadBytes = new Trend('upload_bytes', true)

const manifest = JSON.parse(open('../fixtures/manifest.json'))

const imagePool = manifest.weights.map((entry) => {
  const meta = manifest.files.find((f) => f.path === entry.path)
  const filePath = entry.path.startsWith('fixtures/') ? `../${entry.path}` : entry.path
  return {
    data: open(filePath, 'b'),
    kind: entry.kind,
    contentType: meta.contentType,
    sizeBytes: meta.sizeBytes,
    weight: entry.weight,
  }
})

export function pickImagePayload() {
  let r = Math.random()
  for (const item of imagePool) {
    r -= item.weight
    if (r <= 0) return item
  }
  return imagePool[0]
}

export function fixtureSummary() {
  return imagePool.map((i) => ({
    kind: i.kind,
    kb: Math.round(i.sizeBytes / 1024),
    weight: i.weight,
  }))
}
