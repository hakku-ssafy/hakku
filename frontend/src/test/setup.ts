import '@testing-library/jest-dom'
import { beforeEach } from 'vitest'
import { clearAll as clearResourceCache } from '@/lib/resourceCache'

// 모듈 레벨 SWR 캐시는 테스트 간에 살아남으므로, 각 테스트 전에 비워 격리한다.
beforeEach(() => {
  clearResourceCache()
})

// Node 22+의 네이티브 실험적 localStorage가 jsdom을 가려 clear()가 없는 문제 회피.
// 테스트 전역에 결정적인 인메모리 Storage를 강제 주입한다.
class MemoryStorage implements Storage {
  private store = new Map<string, string>()
  get length(): number { return this.store.size }
  clear(): void { this.store.clear() }
  getItem(key: string): string | null { return this.store.has(key) ? this.store.get(key)! : null }
  key(index: number): string | null { return Array.from(this.store.keys())[index] ?? null }
  removeItem(key: string): void { this.store.delete(key) }
  setItem(key: string, value: string): void { this.store.set(key, String(value)) }
}

Object.defineProperty(globalThis, 'localStorage', {
  value: new MemoryStorage(),
  configurable: true,
  writable: true,
})
